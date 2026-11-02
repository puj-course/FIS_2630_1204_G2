package service.cocina;

import entity.Pedido;
import enums.EstadoPedido;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Adapter: hace que un Pedido (Adaptee) se comporte como una ComandaCocina
 * (Target).
 *
 * Traduce lo que el pedido sabe (estado, fecha, número) a lo que el tablero
 * de cocina necesita (columna, minutos de espera, texto de la acción).
 */
public class PedidoComandaAdapter implements ComandaCocina {

    private final Pedido pedido;

    public PedidoComandaAdapter(Pedido pedido) {
        this.pedido = pedido;
    }

    @Override
    public String getNumero() {
        return pedido.getNumeroPedido();
    }

    @Override
    public Long getMesaId() {
        return pedido.getMesaId();
    }

    @Override
    public long getMinutosEspera() {
        return ChronoUnit.MINUTES.between(pedido.getFechaPedido(), LocalDateTime.now());
    }

    @Override
    public Columna getColumna() {
        return switch (pedido.getEstado()) {
            case EN_PREPARACION -> Columna.PREPARACION;
            case COMPLETADO -> Columna.LISTOS;
            // PENDIENTE y ASIGNADO_MESA se agrupan visualmente como "pendientes"
            default -> Columna.PENDIENTES;
        };
    }

    @Override
    public boolean puedeAvanzar() {
        return pedido.getEstado() != EstadoPedido.COMPLETADO;
    }

    @Override
    public String getTextoAccion() {
        return switch (pedido.getEstado()) {
            case PENDIENTE, ASIGNADO_MESA -> "Pasar a preparación";
            case EN_PREPARACION -> "Marcar como listo";
            default -> "—";
        };
    }

    @Override
    public Pedido avanzar() {
        if (pedido.getEstado() == EstadoPedido.EN_PREPARACION) {
            pedido.setEstado(EstadoPedido.COMPLETADO);
        } else {
            pedido.setEstado(EstadoPedido.EN_PREPARACION);
        }
        return pedido;
    }
}
