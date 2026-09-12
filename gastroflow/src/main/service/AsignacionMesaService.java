package com.restaurante.service;

import com.restaurante.entity.AsignacionMesa;
import com.restaurante.entity.Mesa;
import com.restaurante.exception.AsignacionActivaException;
import com.restaurante.exception.MesaNotFoundException;
import com.restaurante.repository.AsignacionMesaRepository;
import com.restaurante.repository.MesaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AsignacionMesaService {

    private final MesaRepository mesaRepository;
    private final AsignacionMesaRepository asignacionMesaRepository;

    public AsignacionMesaService(
            MesaRepository mesaRepository, AsignacionMesaRepository asignacionMesaRepository) {

        this.mesaRepository = mesaRepository;
        this.asignacionMesaRepository = asignacionMesaRepository;
    }

    public AsignacionMesa asignarMesa(Long mesaId, Long usuarioId) {

        // La mesa debe existir y estar habilitada
        Mesa mesa =
                mesaRepository.findById(mesaId)
                        .orElseThrow(
                                () -> new MesaNotFoundException("Mesa no encontrada con id " + mesaId)
                        );

        if (!mesa.getIsActive().equals(1)) {
            throw new MesaNotFoundException("La mesa " + mesa.getNumeroMesa() + " no está habilitada");
        }

        // La mesa debe estar DISPONIBLE para poder asignarla
        if (!"DISPONIBLE".equals(mesa.getEstado().getCodigoEstado())) {
            throw new AsignacionActivaException(
                    "La mesa " + mesa.getNumeroMesa() + " no está disponible"
            );
        }

        // Evitar una asignación contradictoria: la mesa no debe tener otra asignación activa
        boolean tieneAsignacionActiva =
                asignacionMesaRepository.findByMesaIdAndIsActiveTrue(mesaId).isPresent();

        if (tieneAsignacionActiva) {
            throw new AsignacionActivaException(
                    "La mesa " + mesa.getNumeroMesa() + " ya tiene un mesero asignado"
            );
        }

        AsignacionMesa asignacion = new AsignacionMesa();
        asignacion.setMesa(mesa);
        asignacion.setUsuarioId(usuarioId);

        return asignacionMesaRepository.save(asignacion);
    }

    public List<AsignacionMesa> consultarMesasPorMesero(Long usuarioId) {
        return asignacionMesaRepository.findByUsuarioIdAndIsActiveTrue(usuarioId);
    }

    public AsignacionMesa desactivarAsignacion(Long asignacionId) {

        AsignacionMesa asignacion =
                asignacionMesaRepository.findById(asignacionId)
                        .orElseThrow(
                                () -> new MesaNotFoundException(
                                        "Asignación no encontrada con id " + asignacionId
                                )
                        );

        // Deshabilitación lógica: no se elimina el registro, solo se marca inactivo
        asignacion.setIsActive(false);

        return asignacionMesaRepository.save(asignacion);
    }
}
