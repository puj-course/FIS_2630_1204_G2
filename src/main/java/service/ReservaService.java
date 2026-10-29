package service;

import enums.EstadoPedido;
import entity.Reserva;
import exceptions.ReservaNoCancelableException;
import exceptions.ReservaNotFoundException;
import repository.PedidoRepository;
import repository.ReservaRepository;
import ConexionBD.ConexionBD;
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
                        "Reserva no encontrada con id " + idReserva
                ));

        if (!reserva.isActiva()) {
            throw new ReservaNoCancelableException(
                    "La reserva ya está inactiva"
            );
        }

        boolean tienePedidoEnCurso =
                pedidoRepository.existsByMesaIdAndEstadoNot(
                        reserva.getIdMesa(),
                        EstadoPedido.CANCELADO
                );

        if (tienePedidoEnCurso) {
            throw new ReservaNoCancelableException(
                    "La mesa " + reserva.getNumeroMesa()
                            + " tiene un pedido en curso"
            );
        }

        LocalDateTime ahora = LocalDateTime.now();

        try (java.sql.Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false);

            try {
                reservaRepository.cancelar(conn, idReserva, ahora);

                mesaService.cambiarEstado(
                        conn,
                        reserva.getIdMesa(),
                        "DISPONIBLE",
                        "CANCELAR_RESERVA"
                );

                conn.commit();

                reserva.setActiva(false);
                reserva.setFechaCancelacion(ahora);

                return reserva;

            } catch (SQLException | RuntimeException ex) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    ex.addSuppressed(rollbackEx);
                }

                throw ex;
            }
        }
    }

}