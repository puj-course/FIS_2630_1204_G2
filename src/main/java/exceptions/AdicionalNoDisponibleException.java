package exceptions;

public class AdicionalNoDisponibleException extends RuntimeException {


    /** Requerido por Serializable, que RuntimeException implementa. */
    private static final long serialVersionUID = 8983956767605826719L;

    public AdicionalNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
