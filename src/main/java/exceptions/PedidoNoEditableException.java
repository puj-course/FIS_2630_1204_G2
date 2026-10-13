package exceptions;

public class PedidoNoEditableException extends RuntimeException {

    public PedidoNoEditableException(String mensaje) {
        super(mensaje);
    }
}
