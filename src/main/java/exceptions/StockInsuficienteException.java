package exceptions;

public class StockInsuficienteException extends RuntimeException {


    /** Requerido por Serializable, que RuntimeException implementa. */
    private static final long serialVersionUID = 7505007735565145473L;

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
    }
}