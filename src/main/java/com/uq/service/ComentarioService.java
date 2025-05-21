package com.uq.service;

import com.uq.dto.ComentarioDTO;
import com.uq.exception.ProgramNotFoundException;
import com.uq.exception.UnauthorizedException;
import com.uq.mapper.ComentarioMapper;
import com.uq.model.Comentario;
import com.uq.model.Programa;
import com.uq.repository.ComentarioRepository;
import com.uq.repository.ProgramaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ComentarioService {

    private static final Logger LOGGER = Logger.getLogger(ComentarioService.class.getName());

    @Inject
    ComentarioRepository comentarioRepository;

    @Inject
    ProgramaRepository programaRepository;

    @Inject
    ComentarioMapper comentarioMapper;

    // Metodo para obtener todos los comentarios de un programa específico
    // Verifica que el estudiante autenticado es el dueño del programa.
    public List<ComentarioDTO> listCommentsForProgram(Long programaId, Long authenticatedEstudianteId)
            throws ProgramNotFoundException, UnauthorizedException {

        // 1. Verificar si el programa existe
        Programa programa = programaRepository.findById(programaId);
        if (programa == null) {
            throw new ProgramNotFoundException("Programa no encontrado con ID: " + programaId);
        }

        // 2. Lógica de Autorización: Verificar si el usuario autenticado es el dueño del programa
        if (programa.getEstudiante() == null || !programa.getEstudiante().getId().equals(authenticatedEstudianteId)) {
            LOGGER.log(Level.WARNING, "Intento de acceso no autorizado a comentarios del programa {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
            throw new UnauthorizedException("No tienes permiso para ver los comentarios de este programa.");
        }
        LOGGER.log(Level.INFO, "Acceso autorizado a comentarios del programa {0} para estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});


        // 3. Obtener la lista de comentarios para ese programa
        List<Comentario> comentarios = comentarioRepository.listByProgramaId(programaId);
        LOGGER.log(Level.INFO, "Obtenidos {0} comentarios para programa {1}", new Object[]{comentarios.size(), programaId});


        // 4. Mapear la lista de entidades a lista de DTOs y retornar
        return comentarioMapper.toDTOList(comentarios);
    }
}