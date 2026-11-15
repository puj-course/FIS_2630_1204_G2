package exceptions;

public class MesaOcupadaException extends RuntimeException {


    /** Requerido por Serializable, que RuntimeException implementa. */
    private static final long serialVersionUID = 1356721443748088272L;

    public MesaOcupadaException(String mensaje) {
        super(mensaje);
    }
}
