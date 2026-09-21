package com.restaurante.controller;

import com.restaurante.entity.Reserva;
import com.restaurante.service.ReservaService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PatchMapping("/{id}/cancelar")
    public Reserva cancelarReserva(
            @PathVariable Long id, @RequestParam(required = false) String motivo) {

        return reservaService.cancelarReserva(id, motivo);
    }
}
