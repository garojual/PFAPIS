package com.uq.exception;

/**
 * Excepción lanzada cuando no se encuentra un Programa
 * basándose en un identificador (ID).
 */
public class ProgramNotFoundException extends RuntimeException {

    public ProgramNotFoundException(String message) {
        super(message);
    }
}