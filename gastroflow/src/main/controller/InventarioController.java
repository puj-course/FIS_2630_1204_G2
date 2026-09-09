package com.restaurante.controller;

import com.restaurante.dto.IngredienteStockDTO;
import com.restaurante.entity.Ingrediente;
import com.restaurante.repository.IngredienteRepository;
import com.restaurante.service.InventarioService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventario/prueba")
public class InventarioController {

    private final InventarioService inventarioService;
    private final IngredienteRepository ingredienteRepository;

    public InventarioController(
            InventarioService inventarioService, IngredienteRepository ingredienteRepository
    ) {
        this.inventarioService = inventarioService;
        this.ingredienteRepository = ingredienteRepository;
    }

    @GetMapping("/procesar/{pedidoId}")
    public String procesarPedido(
            @PathVariable Long pedidoId
    ) {

        inventarioService.procesarPedido(pedidoId);

        return "Pedido procesado correctamente";
    }

    @GetMapping("/cancelar/{pedidoId}")
    public String cancelarPedido(
            @PathVariable Long pedidoId
    ) {

        inventarioService.cancelarPedido(pedidoId);

        return "Pedido cancelado y stock restaurado correctamente";
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

        inventarioService.evaluarInventario(ingrediente);

        return "Ingrediente evaluado correctamente";
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