package service;

import entity.EstadoPedido;
import entity.Pedido;
import exceptions.MesaNotFoundException;
import exceptions.MesaOcupadaException;
import exceptions.PedidoNoEditableException;
import repository.PedidoRepository;

import java.sql.SQLException;
import java.util.Set;

public class PedidoService {

    private static final Set<EstadoPedido> ESTADOS_YA_CERRADOS = Set.of(
            EstadoPedido.COMPLETADO,
            EstadoPedido.ENTREGADO,
            EstadoPedido.CANCELADO
    );

    private final PedidoRepository pedidoRepository = new PedidoRepository();
    private final MesaService mesaService = new MesaService();

    public Pedido crearPedido(long mesaId, long usuarioId) throws SQLException {
        boolean tieneComandaActiva =
                pedidoRepository.existsByMesaIdAndEstadoNot(mesaId, EstadoPedido.CANCELADO);

        if (tieneComandaActiva) {
            throw new MesaOcupadaException("La mesa " + mesaId + " ya tiene una comanda activa");
        }

        Pedido pedido = new Pedido();
        pedido.setMesaId(mesaId);
        pedido.setUsuarioId(usuarioId);
        pedido.setNumeroPedido("PED-" + System.currentTimeMillis());
        pedido.setEstado(EstadoPedido.PENDIENTE);

        Pedido guardado = pedidoRepository.save(pedido);

        mesaService.cambiarEstado(mesaId, "OCUPADA", "APERTURA_PEDIDO");

        return guardado;
    }

    public Pedido cerrarPedido(long pedidoId, String codigoEstadoDestinoMesa) throws SQLException {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new MesaNotFoundException("Pedido no encontrado con id " + pedidoId));

        if (ESTADOS_YA_CERRADOS.contains(pedido.getEstado())) {
            throw new PedidoNoEditableException(
                    "El pedido " + pedido.getNumeroPedido() + " ya está en estado " + pedido.getEstado());
        }

        pedido.setEstado(EstadoPedido.COMPLETADO);
        Pedido cerrado = pedidoRepository.save(pedido);

        String destino = codigoEstadoDestinoMesa != null ? codigoEstadoDestinoMesa : "LIBRE";
        mesaService.cambiarEstado(pedido.getMesaId(), destino, "CIERRE_PEDIDO");

        return cerrado;
    }
}