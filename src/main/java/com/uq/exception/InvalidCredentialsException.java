package com.uq.exception;

/**
 * Excepción lanzada cuando las credenciales proporcionadas (ej: email y contraseña)
 * son incorrectas durante un intento de inicio de sesión.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}