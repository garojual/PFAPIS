package com.uq.dto;

import com.uq.enums.DifficultyLevel;
import jakarta.validation.constraints.NotBlank; // Para validaciones
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class EjemploDTO {
    // ID es para la respuesta, no para la solicitud POST de creación
    private Long id;

    @NotBlank(message = "El título no puede estar vacío") // Validar campos obligatorios
    private String titulo;

    @NotBlank(message = "La descripción no puede estar vacía")
    private String descripcion;

    @NotBlank(message = "El código fuente no puede estar vacío")
    private String codigoFuente;

    @NotBlank(message = "El tema no puede estar vacío")
    private String tema;

    // Incluir isShared para que el profesor pueda decidir al crear
    private boolean shared;

    private String profesorNombre;

    private Set<String> tags = new HashSet<>(); // Campo para etiquetas (Set de Strings)
    private DifficultyLevel difficulty; // Campo para el nivel de dificultad (usando el Enum)

    

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }
}