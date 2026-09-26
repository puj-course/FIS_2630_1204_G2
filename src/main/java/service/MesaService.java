package service;

import ConexionDB.ConexionBD;
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

    public void registrarMesa(int numeroMesa) throws SQLException {
        mesaRepository.agregarMesa(numeroMesa);
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
}