package exceptions;

public class ReservaNotFoundException extends RuntimeException {

    public ReservaNotFoundException(String mensaje) {
        super(mensaje);
    }
}
