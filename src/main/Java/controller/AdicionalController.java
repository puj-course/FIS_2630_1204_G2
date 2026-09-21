package com.restaurante.controller;

import com.restaurante.entity.Adicional;
import com.restaurante.entity.DetallePedidoAdicional;
import com.restaurante.repository.AdicionalRepository;
import com.restaurante.service.AdicionalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/adicionales")
public class AdicionalController {

    private final AdicionalRepository adicionalRepository;
    private final AdicionalService adicionalService;

    public AdicionalController(
            AdicionalRepository adicionalRepository, AdicionalService adicionalService) {

        this.adicionalRepository = adicionalRepository;
        this.adicionalService = adicionalService;
    }

    @GetMapping
    public List<Adicional> listarAdicionales() {
        return adicionalRepository.findAll();
    }

    @PostMapping("/detalle-pedido/{detallePedidoId}")
    public DetallePedidoAdicional agregarAdicional(
            @PathVariable Long detallePedidoId,
            @RequestParam Long adicionalId,
            @RequestParam(required = false) Integer cantidad) {

        return adicionalService.agregarAdicional(detallePedidoId, adicionalId, cantidad);
    }
}
