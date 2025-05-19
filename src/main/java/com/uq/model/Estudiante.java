package com.uq.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String email;
    private String contrasena;

    @OneToMany(mappedBy = "estudiante")
    private List<Programa> programas;

    @OneToMany(mappedBy = "estudiante")
    private List<Feedback> feedbacks;
}
