package com.restaurante.exception;

public class MesaNotFoundException extends RuntimeException {

    public MesaNotFoundException(String mensaje) {
        super(mensaje);
    }
}