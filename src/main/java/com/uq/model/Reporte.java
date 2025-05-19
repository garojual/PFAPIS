package com.uq.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
public class Reporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String studentId;
    private String progressDetails;

    @OneToMany(mappedBy = "reporte")
    private List<Programa> programasCompletado;

    @OneToMany(mappedBy = "reporte")
    private List<Programa> programasSinCompletar;

}
