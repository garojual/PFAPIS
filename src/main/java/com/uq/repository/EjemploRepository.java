package com.uq.repository;

import com.uq.model.Ejemplo;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class EjemploRepository implements PanacheRepository<Ejemplo> {

    // Metodo para listar ejemplos compartidos
    public List<Ejemplo> listShared() {
        return list("shared", true);
    }

    // Metodo para listar ejemplos compartidos por tema
    public List<Ejemplo> listSharedByTema(String tema) {
        // Consulta Panache: donde shared es true AND tema coincide
        return list("shared = true and tema = ?1", true, tema);
    }
}