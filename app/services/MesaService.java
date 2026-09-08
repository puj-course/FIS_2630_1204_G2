package com.restaurante.service;

import com.restaurante.entity.EstadoMesa;
import com.restaurante.entity.Mesa;
import com.restaurante.entity.Zona;
import com.restaurante.exception.MesaNotFoundException;
import com.restaurante.repository.EstadoMesaRepository;
import com.restaurante.repository.MesaRepository;
import com.restaurante.repository.ZonaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MesaService {

    private final MesaRepository mesaRepository;
    private final ZonaRepository zonaRepository;
    private final EstadoMesaRepository estadoMesaRepository;

    public MesaService(
            MesaRepository mesaRepository,
            ZonaRepository zonaRepository,
            EstadoMesaRepository estadoMesaRepository) {

        this.mesaRepository = mesaRepository;
        this.zonaRepository = zonaRepository;
        this.estadoMesaRepository = estadoMesaRepository;
    }

    public Mesa registrarMesa(Integer numeroMesa, String codigoMesa, Integer capacidad, Long zonaId) {

        // Validar que la zona exista antes de crear la mesa
        Zona zona = 
            zonaRepository.findById(zonaId)
                .orElseThrow(
                    () -> new MesaNotFoundException("Zona no encontrada con id " + zonaId)
                );

        // Toda mesa nueva nace en estado DISPONIBLE
        EstadoMesa disponible = 
            estadoMesaRepository.findByCodigoEstado("DISPONIBLE").
                orElseThrow(
                    () -> new MesaNotFoundException("El código DISPONIBLE no existe en estados_mesa")
                );

        Mesa mesa = new Mesa();
        mesa.setNumeroMesa(numeroMesa);
        mesa.setCodigoMesa(codigoMesa);
        mesa.setCapacidad(capacidad != null ? capacidad : 2);
        mesa.setZona(zona);
        mesa.setEstado(disponible);

        return mesaRepository.save(mesa);
    }

    public List<Mesa> listarMesas(Long zonaId, String codigoEstado) {

        // Resolver el código de estado recibido contra el catálogo, si vino uno
        EstadoMesa estado =
            codigoEstado != null
                ? estadoMesaRepository.findByCodigoEstado(codigoEstado)
                    .orElseThrow(
                        () -> new MesaNotFoundException("Estado no encontrado: " + codigoEstado)
                    )
                : null;

        // Filtrar según qué combinación de zona/estado llegó
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

    public Mesa actualizarEstado(Long mesaId, String codigoEstadoNuevo) {

        Mesa mesa =
            mesaRepository.findById(mesaId)
                .orElseThrow(
                    () -> new MesaNotFoundException("Mesa no encontrada con id " + mesaId)
                );

        // Validar que el código de estado nuevo exista en el catálogo
        EstadoMesa nuevoEstado =
            estadoMesaRepository.findByCodigoEstado(codigoEstadoNuevo)
                .orElseThrow(
                    () -> new MesaNotFoundException("Estado no encontrado: " + codigoEstadoNuevo)
                );

        mesa.setEstado(nuevoEstado);

        return mesaRepository.save(mesa);
    }
}
