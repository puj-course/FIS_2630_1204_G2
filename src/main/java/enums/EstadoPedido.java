package enums;

public enum EstadoPedido {
    PENDIENTE,
    ASIGNADO_MESA,
    EN_PREPARACION,
    PAGADO,
    COMPLETADO,
    ENTREGADO,
    CANCELADO;

    /**
     * Regla única para determinar un pedido cerrado,
     * impidiendo que reciba modificaciones posteriores o reaperturas.
     */
    public static boolean esEstadoCierre(EstadoPedido estado) {
        if (estado == null) {
            return false;
        }
        return estado == PAGADO || estado == COMPLETADO || estado == ENTREGADO || estado == CANCELADO;
    }
}