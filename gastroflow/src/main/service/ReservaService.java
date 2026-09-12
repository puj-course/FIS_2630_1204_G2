package com.restaurante.service;

import com.restaurante.entity.EstadoMesa;
import com.restaurante.entity.EstadoPedido;
import com.restaurante.entity.EstadoReserva;
import com.restaurante.entity.Mesa;
import com.restaurante.entity.Reserva;
import com.restaurante.exception.ReservaNoCancelableException;
import com.restaurante.exception.ReservaNotFoundException;
import com.restaurante.repository.EstadoMesaRepository;
import com.restaurante.repository.MesaRepository;
import com.restaurante.repository.PedidoRepository;
import com.restaurante.repository.ReservaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final MesaRepository mesaRepository;
    private final EstadoMesaRepository estadoMesaRepository;
    private final PedidoRepository pedidoRepository;

    public ReservaService(
            ReservaRepository reservaRepository,
            MesaRepository mesaRepository,
            EstadoMesaRepository estadoMesaRepository,
            PedidoRepository pedidoRepository) {

        this.reservaRepository = reservaRepository;
        this.mesaRepository = mesaRepository;
        this.estadoMesaRepository = estadoMesaRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional
    public Reserva cancelarReserva(Long reservaId, String motivo) {

        Reserva reserva =
                reservaRepository.findById(reservaId)
                        .orElseThrow(
                                () -> new ReservaNotFoundException(
                                        "Reserva no encontrada con id " + reservaId
                                )
                        );

        // Solo se puede cancelar una reserva que sigue activa
        if (reserva.getEstado() != EstadoReserva.ACTIVA) {
            throw new ReservaNoCancelableException(
                    "La reserva ya está en estado " + reserva.getEstado()
            );
        }

        Mesa mesa = reserva.getMesa();

        // No se puede cancelar si la mesa ya tiene un pedido en curso
        boolean tienePedidoEnCurso =
                pedidoRepository.existsByMesaIdAndEstadoNot(mesa.getId(), EstadoPedido.CANCELADO);

        if (tienePedidoEnCurso) {
            throw new ReservaNoCancelableException(
                    "La mesa " + mesa.getNumeroMesa() + " tiene un pedido en curso"
            );
        }

        // Borrado lógico: se actualiza el estado, no se elimina el registro
        reserva.setEstado(EstadoReserva.CANCELADA);
        reserva.setMotivoCancelacion(motivo);
        reserva.setFechaCancelacion(LocalDateTime.now());

        // Liberar la mesa: pasa de RESERVADA a DISPONIBLE
        EstadoMesa disponible =
                estadoMesaRepository.findByCodigoEstado("DISPONIBLE")
                        .orElseThrow(
                                () -> new ReservaNoCancelableException(
                                        "El código DISPONIBLE no existe en estados_mesa"
                                )
                        );

        mesa.setEstado(disponible);
        mesaRepository.save(mesa);

        return reservaRepository.save(reserva);
    }
}
