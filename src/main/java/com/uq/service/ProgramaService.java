package com.uq.service;

import com.uq.dto.ProgramaDTO;
import com.uq.exception.ProgramNotFoundException;
import com.uq.exception.UnauthorizedException; // Importar nueva excepción
import com.uq.exception.UserNotFoundException;
import com.uq.mapper.ProgramaMapper;
import com.uq.model.Estudiante;
import com.uq.model.Programa;
import com.uq.repository.EstudianteRepository;
import com.uq.repository.ProgramaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ProgramaService {

    @Inject
    ProgramaRepository programaRepository;

    @Inject
    EstudianteRepository estudianteRepository;

    @Inject
    ProgramaMapper programaMapper;


    @Transactional
    public ProgramaDTO createProgram(Long estudianteId, ProgramaDTO programaDTO)
            throws UserNotFoundException {

        Estudiante estudiante = estudianteRepository.findById(estudianteId);
        if (estudiante == null) {
            throw new UserNotFoundException("Estudiante no encontrado con ID: " + estudianteId);
        }

        Programa programa = programaMapper.toEntity(programaDTO);
        programa.setEstudiante(estudiante);

        programaRepository.persist(programa);

        return programaMapper.toDTO(programa);
    }

    public List<ProgramaDTO> getProgramsByEstudianteId(Long estudianteId)
            throws UserNotFoundException {

        Estudiante estudiante = estudianteRepository.findById(estudianteId);
        if (estudiante == null) {
            throw new UserNotFoundException("Estudiante no encontrado con ID: " + estudianteId);
        }

        List<Programa> programas = programaRepository.findByEstudianteId(estudianteId);

        return programaMapper.toDTOList(programas);
    }

    // Modificado para aceptar el ID del usuario autenticado para verificación de propiedad
    public ProgramaDTO getProgramById(Long programaId, Long authenticatedEstudianteId)
            throws ProgramNotFoundException, UnauthorizedException {

        Programa programa = programaRepository.findById(programaId);

        if (programa == null) {
            throw new ProgramNotFoundException("Programa no encontrado con ID: " + programaId);
        }

        // Verificar si el usuario autenticado es el dueño del programa ---
        if (programa.getEstudiante() == null || !programa.getEstudiante().getId().equals(authenticatedEstudianteId)) {
            // TODO: Implementar lógica para permitir a profesores ver cualquier programa si es necesario
            throw new UnauthorizedException("No tienes permiso para ver este programa.");
        }

        return programaMapper.toDTO(programa);
    }

}