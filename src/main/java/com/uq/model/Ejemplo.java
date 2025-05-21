package com.uq.model;

import com.uq.enums.DifficultyLevel;
import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
public class Ejemplo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titulo;
    private String descripcion;
    @Column(columnDefinition = "TEXT") // Para permitir código fuente más largo
    private String codigoFuente;

    private String tema; // Tema del ejemplo (ej: "Estructuras de control", "Funciones")
    private boolean shared = false; // Por defecto, los ejemplos no están compartidos

    @ManyToOne
    @JoinColumn(name = "profesor_id") // Columna FK en la tabla "ejemplo"
    private Profesor profesor; // Relación al profesor que creó el ejemplo


    // Etiquetas: Colección de Strings, almacenada en una tabla separada
    @ElementCollection
    @CollectionTable(name = "ejemplo_tags", joinColumns = @JoinColumn(name = "ejemplo_id")) // Define la tabla de unión
    @Column(name = "tag") // Define el nombre de la columna para los tags en la tabla de unión
    private Set<String> tags = new HashSet<>(); // Inicializar para evitar NullPointerException

    // Nivel de Dificultad: Almacenado como String en DB por defecto
    @Enumerated(EnumType.STRING) // Indica que el enum se almacena como su nombre String
    private DifficultyLevel difficulty; // Campo para el nivel de dificultad


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

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public Profesor getProfesor() {
        return profesor;
    }

    public void setProfesor(Profesor profesor) {
        this.profesor = profesor;
    }
}
