package exceptions;

public class PedidoNoEditableException extends RuntimeException {


    /** Requerido por Serializable, que RuntimeException implementa. */
    private static final long serialVersionUID = 7689912508968875697L;

    public PedidoNoEditableException(String mensaje) {
        super(mensaje);
    }
}
