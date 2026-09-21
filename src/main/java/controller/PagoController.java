package com.restaurante.controller;

import com.restaurante.enums.MetodoPago;
import com.restaurante.entity.Pago;
import com.restaurante.service.PagoService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    // Registrar un pago
    @GetMapping("/registrar/{pedidoId}/{metodo}/{monto}")
    public String registrarPago(
            @PathVariable Long pedidoId,
            @PathVariable MetodoPago metodo,
            @PathVariable BigDecimal monto
    ) {

        try {

            Pago pago = pagoService.registrarPago(
                    pedidoId,
                    metodo,
                    monto
            );

            return "Pago registrado correctamente. "
                    + "ID: " + pago.getId()
                    + " - Metodo: " + pago.getMetodo()
                    + " - Monto: " + pago.getMonto();

        } catch (RuntimeException e) {

            return "Error: " + e.getMessage();
        }
    }

    // Consultar pagos de un pedido
    @GetMapping("/pedido/{pedidoId}")
    public List<Pago> consultarPagos(
            @PathVariable Long pedidoId
    ) {

        return pagoService.consultarPagos(pedidoId);
    }

    // Consultar total pagado
    @GetMapping("/pedido/{pedidoId}/total")
    public String obtenerTotalPagado(
            @PathVariable Long pedidoId
    ) {

        try {

            BigDecimal total =
                    pagoService.obtenerTotalPagado(pedidoId);

            return "Total pagado: " + total;

        } catch (RuntimeException e) {

            return "Error: " + e.getMessage();
        }
    }

    // Consultar saldo pendiente
    @GetMapping("/pedido/{pedidoId}/saldo")
    public String obtenerSaldoPendiente(
            @PathVariable Long pedidoId
    ) {

        try {

            BigDecimal saldo =
                    pagoService.obtenerSaldoPendiente(pedidoId);

            return "Saldo pendiente: " + saldo;

        } catch (RuntimeException e) {

            return "Error: " + e.getMessage();
        }
    }
}