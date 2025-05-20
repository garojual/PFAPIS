package com.uq.repository;

import com.uq.model.Estudiante;
import com.uq.model.Profesor;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class ProfesorRepository implements PanacheRepository<Profesor> {
    public Optional<Profesor> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

}
