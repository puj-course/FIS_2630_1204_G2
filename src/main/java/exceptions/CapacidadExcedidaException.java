package exceptions;

public class CapacidadExcedidaException extends RuntimeException {

    /** Requerido por Serializable, que RuntimeException implementa. */
    private static final long serialVersionUID = 6622250118102396399L;

    public CapacidadExcedidaException() {
        super("La cantidad de comensales supera la capacidad máxima de la mesa ");
    }
}
