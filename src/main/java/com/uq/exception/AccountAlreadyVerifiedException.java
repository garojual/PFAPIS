package com.uq.exception;

/**
 * Excepción lanzada cuando se intenta verificar una cuenta que ya está activa.
 */
public class AccountAlreadyVerifiedException extends RuntimeException {

    public AccountAlreadyVerifiedException(String message) {
        super(message);
    }
}