package exceptions;

public class AsignacionActivaException extends RuntimeException {


    /** Requerido por Serializable, que RuntimeException implementa. */
    private static final long serialVersionUID = 2087115588805590837L;

    public AsignacionActivaException(String mensaje) {
        super(mensaje);
    }
}
