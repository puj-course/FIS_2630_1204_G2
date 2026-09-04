package com.gastroflow.dao;

import com.gastroflow.database.ConexionBD;
import com.gastroflow.model.CuentaPago;

import java.math.BigDecimal;
import java.sql.*;
import java.util.Optional;

public class PagoDAO {

    public Optional<CuentaPago> buscarCuenta(String numeroPedido) throws SQLException {
        String sql = """
                SELECT
                    p.pedido_id,
                    p.numero_pedido,
                    m.numero_mesa,
                    p.subtotal,
                    p.impuestos,
                    p.descuento,
                    p.propina,
                    EXISTS (
                        SELECT 1
                        FROM pagos pg
                        WHERE pg.pedido_id = p.pedido_id
                          AND pg.estado = 'PAGADO'
                    ) AS pagada
                FROM pedidos p
                LEFT JOIN mesas m ON m.id_mesa = p.mesa_id
                WHERE p.numero_pedido = ?
                  AND p.estado <> 'CANCELADO'
                """;

        try (Connection connection = ConexionBD.conectar();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, numeroPedido);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                Integer numeroMesa = rs.getObject("numero_mesa") == null
                        ? null
                        : rs.getInt("numero_mesa");

                return Optional.of(new CuentaPago(
                        rs.getLong("pedido_id"),
                        rs.getString("numero_pedido"),
                        numeroMesa,
                        rs.getBigDecimal("subtotal"),
                        rs.getBigDecimal("impuestos"),
                        rs.getBigDecimal("descuento"),
                        rs.getBigDecimal("propina"),
                        rs.getBoolean("pagada")
                ));
            }
        }
    }

    public void cerrarPago(CuentaPago cuenta,
                            int cajeroId,
                            String motivoDescuento) throws SQLException {

        validarValores(cuenta, motivoDescuento);

        String validarCajero = """
                SELECT 1
                FROM usuarios u
                INNER JOIN roles r ON r.id_rol = u.id_rol
                WHERE u.id_usuario = ?
                  AND u.is_active = 1
                  AND UPPER(r.nombre_rol) = 'CAJERO'
                """;

        String bloquearPedido = """
                SELECT estado
                FROM pedidos
                WHERE pedido_id = ?
                FOR UPDATE
                """;

        String verificarPago = """
                SELECT pago_id
                FROM pagos
                WHERE pedido_id = ?
                  AND estado = 'PAGADO'
                LIMIT 1
                FOR UPDATE
                """;

        String actualizarPedido = """
                UPDATE pedidos
                SET descuento = ?,
                    propina = ?,
                    total = ?,
                    fecha_actualizacion = CURRENT_TIMESTAMP
                WHERE pedido_id = ?
                """;

        String insertarPago = """
                INSERT INTO pagos (
                    pedido_id,
                    usuario_id,
                    subtotal,
                    descuento,
                    impuestos,
                    propina,
                    motivo_descuento,
                    total_pagado,
                    estado
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PAGADO')
                """;

        try (Connection connection = ConexionBD.conectar()) {
            connection.setAutoCommit(false);

            try {
                validarCajero(connection, validarCajero, cajeroId);
                bloquearYValidarPedido(connection, bloquearPedido, cuenta.getPedidoId());
                validarPagoNoDuplicado(connection, verificarPago, cuenta.getPedidoId());

                BigDecimal total = cuenta.calcularTotal();

                try (PreparedStatement update = connection.prepareStatement(actualizarPedido)) {
                    update.setBigDecimal(1, cuenta.getDescuento());
                    update.setBigDecimal(2, cuenta.getPropina());
                    update.setBigDecimal(3, total);
                    update.setLong(4, cuenta.getPedidoId());

                    if (update.executeUpdate() != 1) {
                        throw new SQLException("No fue posible actualizar el pedido.");
                    }
                }

                try (PreparedStatement insert = connection.prepareStatement(insertarPago)) {
                    insert.setLong(1, cuenta.getPedidoId());
                    insert.setInt(2, cajeroId);
                    insert.setBigDecimal(3, cuenta.getSubtotal());
                    insert.setBigDecimal(4, cuenta.getDescuento());
                    insert.setBigDecimal(5, cuenta.getImpuestos());
                    insert.setBigDecimal(6, cuenta.getPropina());

                    if (motivoDescuento == null || motivoDescuento.isBlank()) {
                        insert.setNull(7, Types.VARCHAR);
                    } else {
                        insert.setString(7, motivoDescuento.trim());
                    }

                    insert.setBigDecimal(8, total);
                    insert.executeUpdate();
                }

                connection.commit();

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void validarValores(CuentaPago cuenta, String motivoDescuento) throws SQLException {
        if (cuenta.getDescuento().compareTo(BigDecimal.ZERO) < 0) {
            throw new SQLException("El descuento no puede ser negativo.");
        }

        if (cuenta.getPropina().compareTo(BigDecimal.ZERO) < 0) {
            throw new SQLException("La propina no puede ser negativa.");
        }

        if (cuenta.getDescuento().compareTo(BigDecimal.ZERO) > 0
                && (motivoDescuento == null || motivoDescuento.isBlank())) {
            throw new SQLException("Debe registrar el motivo del descuento.");
        }

        if (cuenta.getDescuento().compareTo(cuenta.getSubtotal()) > 0) {
            throw new SQLException("El descuento no puede superar el subtotal.");
        }

        if (cuenta.calcularTotal().compareTo(BigDecimal.ZERO) < 0) {
            throw new SQLException("El total final no puede ser negativo.");
        }
    }

    private void validarCajero(Connection connection, String sql, int cajeroId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, cajeroId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("El usuario autenticado no existe, está inactivo o no tiene rol Cajero.");
                }
            }
        }
    }

    private void bloquearYValidarPedido(Connection connection, String sql, long pedidoId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, pedidoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("El pedido ya no existe.");
                }
                if ("CANCELADO".equalsIgnoreCase(rs.getString("estado"))) {
                    throw new SQLException("No se puede pagar un pedido cancelado.");
                }
            }
        }
    }

    private void validarPagoNoDuplicado(Connection connection, String sql, long pedidoId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, pedidoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    throw new SQLException("Esta cuenta ya fue pagada.");
                }
            }
        }
    }
}
