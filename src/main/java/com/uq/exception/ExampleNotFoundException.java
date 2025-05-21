package com.uq.exception;

/**
 * Excepción lanzada cuando no se encuentra un Ejemplo
 * basándose en un identificador (ID).
 */
public class ExampleNotFoundException extends RuntimeException {

    public ExampleNotFoundException(String message) {
        super(message);
    }
}