package com.uq.service;

import com.uq.dto.ComentarioDTO;
import com.uq.exception.ProgramNotFoundException;
import com.uq.exception.UnauthorizedException;
import com.uq.exception.UserNotFoundException;
import com.uq.mapper.ComentarioMapper;
import com.uq.model.Comentario;
import com.uq.model.Estudiante;
import com.uq.model.Profesor;
import com.uq.model.Programa;
import com.uq.repository.ComentarioRepository;
import com.uq.repository.ProfesorRepository;
import com.uq.repository.ProgramaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
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

    @Inject
    ProfesorRepository profesorRepository;

    @Inject
    EmailService emailService;


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

    // Metodo para añadir un comentario por parte de un Profesor
    // Este metodo sería llamado desde un ProfesorController
    @Transactional
    public ComentarioDTO addCommentByProfessor(Long programaId, Long profesorId, String commentText)
            throws ProgramNotFoundException, UserNotFoundException {

        // 1. Validar que el programa existe
        Programa programa = programaRepository.findById(programaId);
        if (programa == null) {
            throw new ProgramNotFoundException("Programa no encontrado con ID: " + programaId);
        }

        // 2. Validar que el profesor existe
        Profesor profesor = profesorRepository.findById(profesorId);
        if (profesor == null) {
            // Puedes usar una excepción más específica como ProfesorNotFoundException si la creas
            throw new UserNotFoundException("Profesor no encontrado con ID: " + profesorId);
        }

        // 3. Crear la entidad Comentario
        Comentario comentario = new Comentario();
        comentario.setPrograma(programa); // Relacionar con el programa
        comentario.setProfesor(profesor); // Relacionar con el profesor
        comentario.setTexto(commentText);
        comentario.setFecha(LocalDateTime.now()); // Establecer la fecha actual

        // 4. Persistir el comentario
        comentarioRepository.persist(comentario);
        LOGGER.log(Level.INFO, "Comentario creado por profesor {0} en programa {1}", new Object[]{profesorId, programaId});


        // 5. --- Lógica de Envío de Notificación al Estudiante ---
        // Solo enviamos notificación si el programa tiene un estudiante dueño
        if (programa.getEstudiante() != null && programa.getEstudiante().getEmail() != null) {
            try {
                // Llamar al EmailService para enviar la notificación
                String studentEmail = programa.getEstudiante().getEmail();
                String programTitle = programa.getTitulo(); // Usar el título del programa en el email
                String professorName = profesor.getNombre(); // Usar el nombre del profesor

                emailService.sendCommentNotification(studentEmail, programTitle, programaId, professorName, commentText);
                LOGGER.log(Level.INFO, "Solicitud de envío de notificación de comentario iniciada para estudiante {0}", studentEmail);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al enviar notificación de comentario después de guardar comentario " + comentario.getId(), e);
            }
        } else {
            LOGGER.log(Level.WARNING, "No se pudo enviar notificación para comentario {0}. Programa {1} no tiene estudiante dueño o email.", new Object[]{comentario.getId(), programaId});
        }

        // 6. Mapear la entidad persistida de vuelta a DTO para la respuesta
        return comentarioMapper.toDTO(comentario);
    }
}