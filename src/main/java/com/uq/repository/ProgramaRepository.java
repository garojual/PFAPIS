package com.uq.repository;

import com.uq.model.Programa;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ProgramaRepository implements PanacheRepository<Programa> {

    // Añadimos un método específico para encontrar programas por Estudiante.
    public List<Programa> findByEstudianteId(Long estudianteId) {
        return list("estudiante.id", estudianteId);
    }
}