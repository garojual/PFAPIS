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
import java.util.logging.Level;
import java.util.logging.Logger;


@ApplicationScoped
public class ProgramaService {

    private static final Logger LOGGER = Logger.getLogger(ProgramaService.class.getName()); // Logger para el servicio

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
        programa.setResuelto(false);

        programaRepository.persist(programa);
        LOGGER.log(Level.INFO, "Programa creado con ID {0} para estudiante {1}", new Object[]{programa.getId(), estudianteId});

        return programaMapper.toDTO(programa);
    }

    public List<ProgramaDTO> getProgramsByEstudianteId(Long estudianteId)
            throws UserNotFoundException {

        Estudiante estudiante = estudianteRepository.findById(estudianteId);
        if (estudiante == null) {
            throw new UserNotFoundException("Estudiante no encontrado con ID: " + estudianteId);
        }

        List<Programa> programas = programaRepository.findByEstudianteId(estudianteId);
        LOGGER.log(Level.INFO, "Obtenidos {0} programas para estudiante {1}", new Object[]{programas.size(), estudianteId});

        return programaMapper.toDTOList(programas);
    }

    // Este método ahora verifica la propiedad del programa
    public ProgramaDTO getProgramById(Long programaId, Long authenticatedEstudianteId)
            throws ProgramNotFoundException, UnauthorizedException {

        Programa programa = programaRepository.findById(programaId);

        if (programa == null) {
            throw new ProgramNotFoundException("Programa no encontrado con ID: " + programaId);
        }

        // Lógica de Autorización: Verificar si el usuario autenticado es el dueño del programa
        // Implementar lógica para permitir a profesores ver cualquier programa si es necesario
        if (programa.getEstudiante() == null || !programa.getEstudiante().getId().equals(authenticatedEstudianteId)) {
            LOGGER.log(Level.WARNING, "Intento de acceso no autorizado al programa {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
            throw new UnauthorizedException("No tienes permiso para ver este programa.");
        }
        LOGGER.log(Level.INFO, "Acceso autorizado al programa {0} para estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});


        return programaMapper.toDTO(programa);
    }

    // Metodo para actualizar un programa completo
    @Transactional
    public ProgramaDTO updateProgram(Long programaId, ProgramaDTO updatedProgramaDTO, Long authenticatedEstudianteId)
            throws ProgramNotFoundException, UnauthorizedException {

        Programa existingPrograma = programaRepository.findById(programaId);

        if (existingPrograma == null) {
            throw new ProgramNotFoundException("Programa no encontrado con ID: " + programaId);
        }

        // Lógica de Autorización: Verificar propiedad
        if (existingPrograma.getEstudiante() == null || !existingPrograma.getEstudiante().getId().equals(authenticatedEstudianteId)) {
            LOGGER.log(Level.WARNING, "Intento de actualización no autorizada del programa {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
            throw new UnauthorizedException("No tienes permiso para actualizar este programa.");
        }

        // Usar el mapper para actualizar la entidad existente desde el DTO
        programaMapper.updateEntityFromDto(updatedProgramaDTO, existingPrograma);

        LOGGER.log(Level.INFO, "Programa actualizado con ID {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
        return programaMapper.toDTO(existingPrograma);
    }

    // Metodo para actualizar un programa parcialmente
    @Transactional
    public ProgramaDTO partialUpdateProgram(Long programaId, ProgramaDTO partialProgramaDTO, Long authenticatedEstudianteId)
            throws ProgramNotFoundException, UnauthorizedException {

        Programa existingPrograma = programaRepository.findById(programaId);

        if (existingPrograma == null) {
            throw new ProgramNotFoundException("Programa no encontrado con ID: " + programaId);
        }

        // Lógica de Autorización: Verificar propiedad
        if (existingPrograma.getEstudiante() == null || !existingPrograma.getEstudiante().getId().equals(authenticatedEstudianteId)) {
            LOGGER.log(Level.WARNING, "Intento de actualización parcial no autorizada del programa {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
            throw new UnauthorizedException("No tienes permiso para actualizar este programa.");
        }

        // Actualizar solo los campos no nulos del DTO
        if (partialProgramaDTO.getTitulo() != null) existingPrograma.setTitulo(partialProgramaDTO.getTitulo());
        if (partialProgramaDTO.getDescripcion() != null) existingPrograma.setDescripcion(partialProgramaDTO.getDescripcion());
        if (partialProgramaDTO.getCodigoFuente() != null) existingPrograma.setCodigoFuente(partialProgramaDTO.getCodigoFuente());
        //if (partialProgramaDTO.IsResuelto() != null) existingPrograma.setResuelto(partialProgramaDTO.IsResuelto());
        existingPrograma.setResuelto(partialProgramaDTO.isResuelto());

        LOGGER.log(Level.INFO, "Programa parcialmente actualizado con ID {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
        return programaMapper.toDTO(existingPrograma); // Mapear la entidad actualizada a DTO
    }


    // Metodo para eliminar un programa
    @Transactional
    public void deleteProgram(Long programaId, Long authenticatedEstudianteId)
            throws ProgramNotFoundException, UnauthorizedException {

        Programa existingPrograma = programaRepository.findById(programaId);

        if (existingPrograma == null) {
            throw new ProgramNotFoundException("Programa no encontrado con ID: " + programaId);
        }

        // Lógica de Autorización: Verificar propiedad
        if (existingPrograma.getEstudiante() == null || !existingPrograma.getEstudiante().getId().equals(authenticatedEstudianteId)) {
            LOGGER.log(Level.WARNING, "Intento de eliminación no autorizada del programa {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});
            throw new UnauthorizedException("No tienes permiso para eliminar este programa.");
        }

        // Eliminar el programa. Panache.delete(entity) retorna void si no encuentra, boolean si usa deleteById.
        // Con findById previo, es seguro usar delete(entity).
        programaRepository.delete(existingPrograma);
        LOGGER.log(Level.INFO, "Programa eliminado con ID {0} por estudiante {1}", new Object[]{programaId, authenticatedEstudianteId});

        // No retorna nada si la eliminación fue exitosa y autorizada.
    }
}