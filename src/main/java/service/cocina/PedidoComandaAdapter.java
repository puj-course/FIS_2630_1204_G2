package service.cocina;

import entity.Pedido;
import enums.EstadoPedido;
import exceptions.PedidoNoEditableException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Set;

/**
 * Adapter: hace que un Pedido (Adaptee) se comporte como una ComandaCocina
 * (Target).
 *
 * Traduce lo que el pedido sabe (estado, fecha, número) a lo que el tablero
 * de cocina necesita (columna, minutos de espera, texto de la acción).
 */
public class PedidoComandaAdapter implements ComandaCocina {

    /**
     * Taller de pruebas: los unicos estados que tienen un siguiente paso en
     * cocina. Antes la regla estaba escrita al reves, como "cualquier estado
     * menos COMPLETADO", de modo que un pedido ENTREGADO, PAGADO o CANCELADO
     * respondia que si podia avanzar. En la pantalla no se notaba porque la
     * consulta findPedidosParaCocina nunca trae esos estados, pero el servicio
     * quedaba sin contrato propio y dependia de que la consulta no cambiara.
     */
    private static final Set<EstadoPedido> ESTADOS_EN_COCINA = EnumSet.of(
            EstadoPedido.PENDIENTE,
            EstadoPedido.ASIGNADO_MESA,
            EstadoPedido.EN_PREPARACION
    );

    private final Pedido pedido;

    public PedidoComandaAdapter(Pedido pedido) {
        if (pedido == null) {
            throw new IllegalArgumentException("La comanda necesita un pedido");
        }
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

    /**
     * Un pedido sin fecha registrada devuelve 0 minutos. Antes lanzaba
     * NullPointerException y tumbaba el refresco completo del tablero, que
     * corre cada pocos segundos, por una sola fila con el dato incompleto.
     */
    @Override
    public long getMinutosEspera() {
        LocalDateTime inicio = pedido.getFechaPedido();
        if (inicio == null) {
            return 0L;
        }
        long minutos = ChronoUnit.MINUTES.between(inicio, LocalDateTime.now());
        return Math.max(minutos, 0L);
    }

    @Override
    public Columna getColumna() {
        if (pedido.getEstado() == null) {
            return Columna.PENDIENTES;
        }
        return switch (pedido.getEstado()) {
            case EN_PREPARACION -> Columna.PREPARACION;
            case COMPLETADO -> Columna.LISTOS;
            // PENDIENTE y ASIGNADO_MESA se agrupan visualmente como "pendientes"
            default -> Columna.PENDIENTES;
        };
    }

    @Override
    public boolean puedeAvanzar() {
        return ESTADOS_EN_COCINA.contains(pedido.getEstado());
    }

    @Override
    public String getTextoAccion() {
        if (pedido.getEstado() == null) {
            return "—";
        }
        return switch (pedido.getEstado()) {
            case PENDIENTE, ASIGNADO_MESA -> "Pasar a preparación";
            case EN_PREPARACION -> "Marcar como listo";
            default -> "—";
        };
    }

    @Override
    public Pedido avanzar() {
        if (!puedeAvanzar()) {
            throw new PedidoNoEditableException(
                    "El pedido " + pedido.getNumeroPedido()
                            + " ya no tiene pasos pendientes en cocina (estado "
                            + pedido.getEstado() + ")");
        }
        if (pedido.getEstado() == EstadoPedido.EN_PREPARACION) {
            pedido.setEstado(EstadoPedido.COMPLETADO);
        } else {
            pedido.setEstado(EstadoPedido.EN_PREPARACION);
        }
        return pedido;
    }
}
