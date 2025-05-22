package com.uq.repository;


import com.uq.model.Profesor;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class ProfesorRepository implements PanacheRepository<Profesor> {
    public Optional<Profesor> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }
}
