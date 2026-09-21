package com.restaurante.controller;

import com.restaurante.entity.AsignacionMesa;
import com.restaurante.service.AsignacionMesaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asignaciones-mesa")
public class AsignacionMesaController {

    private final AsignacionMesaService asignacionMesaService;

    public AsignacionMesaController(AsignacionMesaService asignacionMesaService) {
        this.asignacionMesaService = asignacionMesaService;
    }

    @PostMapping
    public AsignacionMesa asignarMesa(
            @RequestParam Long mesaId, @RequestParam Long usuarioId) {

        return asignacionMesaService.asignarMesa(mesaId, usuarioId);
    }

    @GetMapping("/mesero/{usuarioId}")
    public List<AsignacionMesa> consultarMesasPorMesero(@PathVariable Long usuarioId) {
        return asignacionMesaService.consultarMesasPorMesero(usuarioId);
    }

    @PatchMapping("/{id}/desactivar")
    public AsignacionMesa desactivarAsignacion(@PathVariable Long id) {
        return asignacionMesaService.desactivarAsignacion(id);
    }
}
