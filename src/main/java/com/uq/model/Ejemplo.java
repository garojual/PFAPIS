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
    private String codigo;
    private String categoria; // e.g. POO, Control Structures
    private String nivel; // e.g. Beginner, Advanced

    @ManyToOne
    @JoinColumn(name = "profesor_id")
    private Profesor profesor;
}
