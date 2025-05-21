package com.uq.exception;

/**
 * Excepción lanzada cuando ocurre un error durante la compilación o ejecución
 * del código de un programa.
 */
public class ProgramExecutionException extends RuntimeException {


    public ProgramExecutionException(String message) {
        super(message);
    }

    public ProgramExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProgramExecutionException(String message, String compileStderr) {
        super(message + "\n" + compileStderr);
    }
}