package service;

import entity.AsignacionMesa;
import exceptions.AsignacionActivaException;
import repository.AsignacionMesaRepository;

import java.sql.SQLException;

public class AsignacionMesaService {

    private final AsignacionMesaRepository asignacionMesaRepository = new AsignacionMesaRepository();

    public AsignacionMesa asignar(long mesaId, long usuarioId) throws SQLException {
        if (asignacionMesaRepository.findActivaByMesaId(mesaId).isPresent()) {
            throw new AsignacionActivaException("La mesa " + mesaId + " ya tiene una asignación activa");
        }

        AsignacionMesa a = new AsignacionMesa();
        a.setMesaId(mesaId);
        a.setUsuarioId(usuarioId);
        a.setIsActive(true);

        return asignacionMesaRepository.save(a);
    }

    public void liberar(long mesaId) throws SQLException {
        AsignacionMesa a = asignacionMesaRepository.findActivaByMesaId(mesaId)
                .orElse(null);
        if (a == null) {
            return;
        }
        a.setIsActive(false);
        asignacionMesaRepository.save(a);
    }
}