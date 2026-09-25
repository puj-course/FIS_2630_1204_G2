package exceptions;

public class ReservaNoCancelableException extends RuntimeException {

    public ReservaNoCancelableException(String mensaje) {
        super(mensaje);
    }
}
