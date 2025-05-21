package com.uq.repository;

import com.uq.model.Comentario;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ComentarioRepository implements PanacheRepository<Comentario> {

    // Metodo para listar comentarios por Programa
    public List<Comentario> listByProgramaId(Long programaId) {
        // Ordenar por fecha para que los comentarios más antiguos aparezcan primero
        return list("programa.id = ?1 order by fecha asc", programaId);
    }
}