package enums;

public enum EstadoPedido {
    PENDIENTE,
    ASIGNADO_MESA,
    EN_PREPARACION,
    PAGADO,
    COMPLETADO,
    ENTREGADO,
    CANCELADO;

    public static boolean esEstadoCierre(EstadoPedido estado) {
        if (estado == null) {
            return false;
        }
        return estado == COMPLETADO || estado == ENTREGADO || estado == CANCELADO;
    }
}