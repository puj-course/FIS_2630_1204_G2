package com.restaurante.service;

import com.restaurante.entity.EstadoMesa;
import com.restaurante.entity.Mesa;
import com.restaurante.entity.Zona;
import com.restaurante.exception.MesaNotFoundException;
import com.restaurante.repository.MesaRepository;
import com.restaurante.repository.ZonaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MesaService {

    private final MesaRepository mesaRepository;
    private final ZonaRepository zonaRepository;

    public MesaService(MesaRepository mesaRepository, ZonaRepository zonaRepository) {
        this.mesaRepository = mesaRepository;
        this.zonaRepository = zonaRepository;
    }

    public Mesa registrarMesa(Integer numeroMesa, String codigoMesa, Integer capacidad, Long zonaId) {

        Zona zona =
                zonaRepository.findById(zonaId)
                        .orElseThrow(
                                () -> new MesaNotFoundException(
                                        "Zona no encontrada con id " + zonaId
                                )
                        );

        Mesa mesa = new Mesa();
        mesa.setNumeroMesa(numeroMesa);
        mesa.setCodigoMesa(codigoMesa);
        mesa.setCapacidad(capacidad != null ? capacidad : 2);
        mesa.setZona(zona);

        return mesaRepository.save(mesa);
    }

    public List<Mesa> listarMesas(Long zonaId, EstadoMesa estado) {

        if (zonaId != null && estado != null) {
            return mesaRepository.findByZonaIdAndEstado(zonaId, estado);
        }
        if (zonaId != null) {
            return mesaRepository.findByZonaId(zonaId);
        }
        if (estado != null) {
            return mesaRepository.findByEstado(estado);
        }
        return mesaRepository.findAll();
    }

    public Mesa actualizarEstado(Long mesaId, EstadoMesa nuevoEstado) {

        Mesa mesa =
                mesaRepository.findById(mesaId)
                        .orElseThrow(
                                () -> new MesaNotFoundException(
                                        "Mesa no encontrada con id " + mesaId
                                )
                        );

        mesa.setEstado(nuevoEstado);

        return mesaRepository.save(mesa);
    }
}
