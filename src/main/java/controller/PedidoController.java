package com.restaurante.controller;

import com.restaurante.entity.DetallePedido;
import com.restaurante.entity.Pedido;
import com.restaurante.service.PedidoService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // Crear un pedido
    @GetMapping("/crear")
    public String crearPedido() {

        Pedido pedido = pedidoService.crearPedido();

        return "Pedido creado correctamente. ID: "
                + pedido.getId()
                + " - Estado: "
                + pedido.getEstado();
    }

    // Agregar un plato
    @GetMapping("/{pedidoId}/agregar/{platoId}/{cantidad}")
    public String agregarPlato(
            @PathVariable Long pedidoId,
            @PathVariable Long platoId,
            @PathVariable BigDecimal cantidad
    ) {

        DetallePedido detalle =
                pedidoService.agregarPlato(
                        pedidoId,
                        platoId,
                        cantidad
                );

        return "Plato agregado correctamente. "
                + "Pedido: " + pedidoId
                + " - Plato: " + detalle.getPlato().getNombre()
                + " - Cantidad: " + detalle.getCantidad();
    }

    // Cambiar cantidad
    @GetMapping("/{pedidoId}/cantidad/{platoId}/{cantidad}")
    public String cambiarCantidad(
            @PathVariable Long pedidoId,
            @PathVariable Long platoId,
            @PathVariable BigDecimal cantidad
    ) {

        DetallePedido detalle =
                pedidoService.cambiarCantidad(
                        pedidoId,
                        platoId,
                        cantidad
                );

        return "Cantidad actualizada correctamente. "
                + "Plato: " + detalle.getPlato().getNombre()
                + " - Nueva cantidad: "
                + detalle.getCantidad();
    }

    // Eliminar plato
    @GetMapping("/{pedidoId}/eliminar/{platoId}")
    public String eliminarPlato(
            @PathVariable Long pedidoId,
            @PathVariable Long platoId
    ) {

        pedidoService.eliminarPlato(
                pedidoId,
                platoId
        );

        return "Plato eliminado correctamente del pedido";
    }

    // Consultar pedido
    @GetMapping("/{pedidoId}")
    public String consultarPedido(
            @PathVariable Long pedidoId
    ) {

        List<DetallePedido> detalles =
                pedidoService.consultarDetalles(pedidoId);

        StringBuilder respuesta = new StringBuilder();

        respuesta.append("Pedido: ")
                .append(pedidoId)
                .append("\n");

        if (detalles.isEmpty()) {
            respuesta.append("El pedido no tiene platos.");
        } else {

            respuesta.append("Platos:\n");

            for (DetallePedido detalle : detalles) {

                respuesta.append("- ")
                        .append(detalle.getPlato().getNombre())
                        .append(" x ")
                        .append(detalle.getCantidad())
                        .append("\n");
            }
        }

        return respuesta.toString();
    }
}