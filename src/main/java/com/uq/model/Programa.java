package com.uq.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Programa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titulo;
    private String descripcion;
    private String codigoFuente;
    private boolean isResuelto;

    private boolean isShared;

    @ManyToOne
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    @OneToMany(mappedBy = "programa")
    private List<Feedback> feedbacks;

    @ManyToOne
    @JoinColumn(name = "reporte_id")  // columna FK en la tabla "programa"
    private Reporte reporte;
}
