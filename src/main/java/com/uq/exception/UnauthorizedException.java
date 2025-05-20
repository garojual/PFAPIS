package com.uq.exception;

/**
 * Excepción lanzada cuando un usuario autenticado intenta acceder a un recurso
 * para el cual no tiene permisos.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}