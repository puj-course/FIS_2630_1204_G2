package com.restaurante.controller;

import com.restaurante.dto.IngredienteStockDTO;
import com.restaurante.service.InventarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventario")
public class InventarioConsultaController {

    private final InventarioService inventarioService;

    public InventarioConsultaController(
            InventarioService inventarioService
    ) {
        this.inventarioService = inventarioService;
    }

    @GetMapping("/ingredientes")
    public Page<IngredienteStockDTO> consultarStock(

            @RequestParam(required = false)
            String nombre,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size

    ) {

        PageRequest pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by("nombre").ascending()
                );

        return inventarioService.consultarStock(
                nombre,
                pageable
        );
    }
}