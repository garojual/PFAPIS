package com.uq.model;

import jakarta.persistence.*;
import lombok.Data;

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
