package models;

public class MesaNotFoundException extends RuntimeException {

    public MesaNotFoundException(String mensaje) {
        super(mensaje);
    }
}
