package com.restaurante.controller;

import com.restaurante.entity.Ingrediente;
import com.restaurante.repository.IngredienteRepository;
import com.restaurante.service.AlertaInventarioService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prueba-alerta")
public class AlertaInventarioController {

    private final IngredienteRepository ingredienteRepository;
    private final AlertaInventarioService alertaService;

    public AlertaInventarioController(
            IngredienteRepository ingredienteRepository,
            AlertaInventarioService alertaService) {

        this.ingredienteRepository = ingredienteRepository;
        this.alertaService = alertaService;
    }

    @PostMapping("/{id}")
    public String evaluarIngrediente(@PathVariable Long id) {

        Ingrediente ingrediente =
                ingredienteRepository.findById(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Ingrediente no encontrado"
                                )
                        );

        alertaService.evaluarInventario(ingrediente);

        return "Ingrediente evaluado correctamente";
    }

    @GetMapping("/{id}")
    public String evaluarIngredienteGet(@PathVariable Long id) {

        Ingrediente ingrediente =
                ingredienteRepository.findById(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Ingrediente no encontrado"
                                )
                        );

        alertaService.evaluarInventario(ingrediente);

        return "Ingrediente evaluado correctamente";
    }
}