package service;

import ConexionBD.ConexionBD;
import entity.EstadoMesa;
import entity.HistorialEstadoMesa;
import entity.Mesa;
import entity.Zona;
import exceptions.MesaNotFoundException;
import repository.EstadoMesaRepository;
import repository.HistorialEstadoMesaRepository;
import repository.MesaRepository;
import repository.ZonaRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class MesaService {

    private final MesaRepository mesaRepository = new MesaRepository();
    private final ZonaRepository zonaRepository = new ZonaRepository();
    private final EstadoMesaRepository estadoMesaRepository = new EstadoMesaRepository();
    private final HistorialEstadoMesaRepository historialEstadoMesaRepository = new HistorialEstadoMesaRepository();

    public void registrarMesa(int numeroMesa, int capacidad) throws SQLException {
        if (numeroMesa < 1 || numeroMesa > 9999) {
            throw new IllegalArgumentException("El número de mesa debe estar entre 1 y 9999.");
        }
        if (capacidad <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser mayor que cero.");
        }
        mesaRepository.agregarMesa(numeroMesa, capacidad);
    }

    public void cambiarCapacidad(int idMesa, int capacidad) throws SQLException {
        if (capacidad <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser mayor que cero.");
        }
        mesaRepository.cambiarCapacidadMesa(idMesa, capacidad);
    }

    public List<Mesa> listarMesas() throws SQLException {
        return mesaRepository.obtenerTodas();
    }

    public void quitarMesa(int idMesa) throws SQLException {
        mesaRepository.quitarMesa(idMesa);
    }

    // Cambia el estado de una mesa y deja registro auditado en historial_estado_mesa.ddl,
    // todo dentro de una única transacción: si el registro de auditoría falla,
    // el cambio de estado de la mesa también se revierte.
    public void cambiarEstado(long mesaId, String codigoEstadoNuevo, String motivo) throws SQLException {

        Mesa mesaActual = mesaRepository.findById((int) mesaId)
                .orElseThrow(() -> new MesaNotFoundException("Mesa no encontrada con id " + mesaId));

        EstadoMesa nuevo = estadoMesaRepository.findByCodigoEstado(codigoEstadoNuevo)
                .orElseThrow(() -> new MesaNotFoundException(
                        "Estado no encontrado: " + codigoEstadoNuevo));

        try (Connection conn = ConexionBD.getConnection()) {

            conn.setAutoCommit(false);

            try {
                mesaRepository.cambiarEstadoMesa(conn, (int) mesaId, codigoEstadoNuevo);

                HistorialEstadoMesa h = new HistorialEstadoMesa();
                h.setMesaId(mesaId);
                h.setEstadoAnteriorId((long) mesaActual.getIdEstadoMesa());
                h.setEstadoNuevoId(nuevo.getId());
                h.setMotivo(motivo);

                historialEstadoMesaRepository.save(conn, h);

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public Zona obtenerZona(long zonaId) throws SQLException {
        return zonaRepository.findById(zonaId)
                .orElseThrow(() -> new MesaNotFoundException("Zona no encontrada con id " + zonaId));
    }

    public EstadoMesa obtenerEstadoPorCodigo(String codigo) throws SQLException {
        return estadoMesaRepository.findByCodigoEstado(codigo)
                .orElseThrow(() -> new MesaNotFoundException("Estado no encontrado: " + codigo));
    }

    public void actualizarCantidadComensales(long mesaId, int nuevaCantidad) throws SQLException {
        if (nuevaCantidad <= 0) {
            throw new IllegalArgumentException("La cantidad de comensales debe ser mayor a cero.");
        }

        Mesa mesa = mesaRepository.findById((int) mesaId)
            .orElseThrow(() -> new MesaNotFoundException("La mesa con ID " + mesaId + " no existe."));

        if (nuevaCantidad > mesa.getCapacidad()) {
            throw new IllegalArgumentException("La cantidad supera la capacidad máxima de la mesa (" + mesa.getCapacidad() + ").");
        }
        mesaRepository.actualizarComensales(mesaId, nuevaCantidad);
    }
}