package service;

import entity.EstadoPedido;
import entity.Reserva;
import exceptions.ReservaNoCancelableException;
import exceptions.ReservaNotFoundException;
import repository.PedidoRepository;
import repository.ReservaRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class ReservaService {

    private final ReservaRepository reservaRepository = new ReservaRepository();
    private final PedidoRepository pedidoRepository = new PedidoRepository();
    private final MesaService mesaService = new MesaService();

    public List<Reserva> listarMesasReservadas() throws SQLException {
        return reservaRepository.obtenerMesasReservadas();
    }

    public List<Reserva> listarReservasDelTurnoActual() throws SQLException {
        return reservaRepository.obtenerReservasDelTurnoActual();
    }

    // HU-055: cancelar una reserva vigente
    public Reserva cancelarReserva(int idReserva) throws SQLException {

        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ReservaNotFoundException(
                        "Reserva no encontrada con id " + idReserva));

        if (!reserva.isActiva()) {
            throw new ReservaNoCancelableException("La reserva ya está inactiva");
        }

        // No se puede cancelar si la mesa ya tiene un pedido en curso
        boolean tienePedidoEnCurso = pedidoRepository.existsByMesaIdAndEstadoNot(
                reserva.getIdMesa(), EstadoPedido.CANCELADO);

        if (tienePedidoEnCurso) {
            throw new ReservaNoCancelableException(
                    "La mesa " + reserva.getNumeroMesa() + " tiene un pedido en curso");
        }

        LocalDateTime ahora = LocalDateTime.now();

        reservaRepository.cancelar(idReserva, ahora);

        // Liberar la mesa: pasa de RESERVADA a LIBRE, con auditoría en historial_estado_mesa.ddl
        mesaService.cambiarEstado(reserva.getIdMesa(), "LIBRE", "CANCELAR_RESERVA");

        reserva.setActiva(false);
        reserva.setFechaCancelacion(ahora);

        return reserva;
    }
}