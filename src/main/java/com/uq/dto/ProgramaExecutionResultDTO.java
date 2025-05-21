package com.uq.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgramaExecutionResultDTO {
    private String stdout; // Salida estándar del programa
    private String stderr; // Salida de error del programa
    private int exitCode;  // Código de salida del proceso (0 = éxito típicamente)
    private String errorMessage; // Mensaje de error de la plataforma (ej: error de compilación, error interno)
    private long durationMillis; // Duración de la ejecución en milisegundos (opcional)
}