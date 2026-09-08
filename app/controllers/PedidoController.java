package com.restaurante.controller;

import com.restaurante.entity.Pedido;
import com.restaurante.service.PedidoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    public Pedido crearPedido(@RequestParam Long mesaId, @RequestParam Long usuarioId) {
        return pedidoService.crearPedido(mesaId, usuarioId);
    }
}
