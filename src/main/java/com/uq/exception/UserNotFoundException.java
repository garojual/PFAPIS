package com.uq.exception;

/**
 * Excepción lanzada cuando no se encuentra un usuario (Estudiante o Profesor)
 * basándose en un identificador (ID, email, etc.).
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}