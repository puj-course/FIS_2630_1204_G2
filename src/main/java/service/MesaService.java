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

    public void cambiarEstado(long mesaId, String codigoEstadoNuevo, String motivo) throws SQLException {
        EstadoMesa nuevo = estadoMesaRepository.findByCodigoEstado(codigoEstadoNuevo)
                .orElseThrow(() -> new MesaNotFoundException(
                        "Estado no encontrado: " + codigoEstadoNuevo));

        mesaRepository.cambiarEstadoMesa((int) mesaId, codigoEstadoNuevo);

        HistorialEstadoMesa h = new HistorialEstadoMesa();
        h.setMesaId(mesaId);
        h.setEstadoNuevoId(nuevo.getId());
        // estadoAnteriorId queda null si no lo resolvemos aquí
        try {
            historialEstadoMesaRepository.save(h);
        } catch (SQLException ignored) {

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