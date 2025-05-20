package com.uq.exception;

/**
 * Excepción lanzada cuando se intenta realizar una operación (ej: login)
 * con una cuenta que no ha sido activada (verificada).
 */
public class InactiveAccountException extends RuntimeException {

    public InactiveAccountException(String message) {
        super(message);
    }
}