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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCodigoFuente() {
        return codigoFuente;
    }

    public void setCodigoFuente(String codigoFuente) {
        this.codigoFuente = codigoFuente;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema;
    }

    public String getProfesorNombre() {
        return profesorNombre;
    }

    public void setProfesorNombre(String profesorNombre) {
        this.profesorNombre = profesorNombre;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }
}