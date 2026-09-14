package com.restaurante.controller;

import com.restaurante.enums.EstadoMesa;
import com.restaurante.entity.Mesa;
import com.restaurante.entity.Mesero;
import com.restaurante.repository.MesaRepository;
import com.restaurante.repository.MeseroRepository;
import com.restaurante.service.MesaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mesas")
public class MesaController {

    private final MesaService mesaService;
    private final MesaRepository mesaRepository;
    private final MeseroRepository meseroRepository;

    public MesaController(
            MesaService mesaService,
            MesaRepository mesaRepository,
            MeseroRepository meseroRepository
    ) {
        this.mesaService = mesaService;
        this.mesaRepository = mesaRepository;
        this.meseroRepository = meseroRepository;
    }

    // Crear una mesa
    @GetMapping("/crear/{numero}")
    public String crearMesa(@PathVariable Integer numero) {

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
    public String reasignarMesa(@PathVariable Long pedidoId, @PathVariable Long nuevaMesaId) {

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
    @GetMapping("/alerta-demora/{mesaId}")
    public String verificarDemora(@PathVariable Long mesaId) {
        try {
            return mesaService.verificarDemora(mesaId);
        } catch (RuntimeException e) {
            return "Error: " + e.getMessage();
        }
    }
    @GetMapping("/alertas-demora")
    public String verificarTodasLasDemoras() {
        try {
            return mesaService.verificarTodasLasDemoras();
        } catch (RuntimeException e) {
            return "Error: " + e.getMessage();
        }
    }
    @GetMapping("/pedido/{mesaId}")
    public String consultarPedidoDeMesa(@PathVariable Long mesaId) {
        try {
            return mesaService.consultarPedidoDeMesa(mesaId);
        } catch (RuntimeException e) {
            return "Error: " + e.getMessage();
        }
    }
    @GetMapping("/mesero/{mesaId}")
    public String consultarMesero(@PathVariable Long mesaId) {
        try {
            return mesaService.consultarMesero(mesaId);
        } catch (RuntimeException e) {
            return "Error: " + e.getMessage();
        }
    }
    @GetMapping("/mesero/crear/{nombre}")
    public String crearMesero(@PathVariable String nombre) {
        Mesero mesero = new Mesero();
        mesero.setNombre(nombre);
        meseroRepository.save(mesero);
        return "Mesero creado correctamente. ID: "
                + mesero.getId();
    }
}