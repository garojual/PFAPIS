package com.uq.exception;

/**
 * Excepción lanzada cuando el código de verificación proporcionado es incorrecto
 * para el usuario especificado.
 */
public class InvalidVerificationCodeException extends RuntimeException {

    public InvalidVerificationCodeException(String message) {
        super(message);
    }
}