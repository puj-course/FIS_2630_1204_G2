package com.restaurante.exception;

public class ReservaNotFoundException extends RuntimeException {

    public ReservaNotFoundException(String mensaje) {
        super(mensaje);
    }
}
