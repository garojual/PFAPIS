package com.uq.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
public class Profesor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String email;
    private String contrasena;

    @OneToMany(mappedBy = "profesor")
    private List<Feedback> feedbacks;

    @OneToMany(mappedBy = "profesor")
    private List<Ejemplo> ejemplos;

    // Getters and setters
}
