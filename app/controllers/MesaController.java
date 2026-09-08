package com.restaurante.controller;

import com.restaurante.entity.EstadoMesa;
import com.restaurante.entity.Mesa;
import com.restaurante.repository.MesaRepository;
import com.restaurante.service.MesaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mesas")
public class MesaController {

    private final MesaService mesaService;
    private final MesaRepository mesaRepository;

    public MesaController(
            MesaService mesaService,
            MesaRepository mesaRepository
    ) {
        this.mesaService = mesaService;
        this.mesaRepository = mesaRepository;
    }

    // Crear una mesa
    @GetMapping("/crear/{numero}")
    public String crearMesa(
            @PathVariable Integer numero
    ) {

        if (mesaRepository.existsByNumero(numero)) {
            return "Ya existe una mesa con el numero " + numero;
        }

        Mesa mesa = new Mesa();
        mesa.setNumero(numero);
        mesa.setEstado(EstadoMesa.DISPONIBLE);

        mesaRepository.save(mesa);

        return "Mesa creada correctamente. ID: "
                + mesa.getId()
                + " - Numero: "
                + mesa.getNumero();
    }

    // Consultar todas las mesas
    @GetMapping
    public List<Mesa> consultarMesas() {
        return mesaRepository.findAll();
    }

    // Consultar mesas disponibles
    @GetMapping("/disponibles")
    public List<Mesa> consultarMesasDisponibles() {
        return mesaRepository.findByEstado(
                EstadoMesa.DISPONIBLE
        );
    }

    // Reasignar un pedido a otra mesa
    @GetMapping("/reasignar/{pedidoId}/{nuevaMesaId}")
    public String reasignarMesa(
            @PathVariable Long pedidoId,
            @PathVariable Long nuevaMesaId
    ) {

        try {

            mesaService.reasignarMesa(
                    pedidoId,
                    nuevaMesaId
            );

            return "Mesa reasignada correctamente. "
                    + "Pedido " + pedidoId
                    + " ahora esta en la mesa "
                    + nuevaMesaId;

        } catch (RuntimeException e) {

            return "Error: " + e.getMessage();
        }
    }
}