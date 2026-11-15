package exceptions;

public class ReservaNoCancelableException extends RuntimeException {


    /** Requerido por Serializable, que RuntimeException implementa. */
    private static final long serialVersionUID = 5017689242706551829L;

    public ReservaNoCancelableException(String mensaje) {
        super(mensaje);
    }
}
