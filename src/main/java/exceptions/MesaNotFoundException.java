package exceptions;

public class MesaNotFoundException extends RuntimeException {


    /** Requerido por Serializable, que RuntimeException implementa. */
    private static final long serialVersionUID = 358642099310427552L;

    public MesaNotFoundException(String mensaje) {
        super(mensaje);
    }
}
