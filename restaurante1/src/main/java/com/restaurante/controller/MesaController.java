package com.restaurante.controller;

import com.restaurante.entity.EstadoMesa;
import com.restaurante.entity.Mesa;
import com.restaurante.service.MesaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mesas")
public class MesaController {

    private final MesaService mesaService;

    public MesaController(MesaService mesaService) {
        this.mesaService = mesaService;
    }

    @PostMapping
    public Mesa registrarMesa(
            @RequestParam Integer numeroMesa,
            @RequestParam(required = false) String codigoMesa,
            @RequestParam(required = false) Integer capacidad,
            @RequestParam Long zonaId) {

        return mesaService.registrarMesa(numeroMesa, codigoMesa, capacidad, zonaId);
    }

    @GetMapping
    public List<Mesa> listarMesas(
            @RequestParam(required = false) Long zonaId,
            @RequestParam(required = false) EstadoMesa estado) {

        return mesaService.listarMesas(zonaId, estado);
    }

    @PatchMapping("/{id}/estado")
    public Mesa actualizarEstado(
            @PathVariable Long id,
            @RequestParam EstadoMesa estado) {

        return mesaService.actualizarEstado(id, estado);
    }
}
