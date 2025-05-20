package com.uq.exception;

/**
 * Excepción lanzada cuando el código de verificación proporcionado ha expirado.
 */
public class ExpiredVerificationCodeException extends RuntimeException {

    public ExpiredVerificationCodeException(String message) {
        super(message);
    }
}