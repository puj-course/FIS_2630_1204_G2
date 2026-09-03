package com.restaurante.controller;

import com.restaurante.service.InventarioService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventario/prueba")
public class InventarioPruebaController {

    private final InventarioService inventarioService;

    public InventarioPruebaController(
            InventarioService inventarioService
    ) {
        this.inventarioService = inventarioService;
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
}