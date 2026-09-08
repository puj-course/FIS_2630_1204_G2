package com.restaurante.exception;

public class MesaOcupadaException extends RuntimeException {

    public MesaOcupadaException(String mensaje) {
        super(mensaje);
    }
}
