package com.uq.repository;

import com.uq.model.Estudiante;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class EstudianteRepository implements PanacheRepository<Estudiante> {
    public Optional<Estudiante> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

}
