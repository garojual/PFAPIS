package com.uq.service;

import com.uq.dto.EjemploDTO;
import com.uq.exception.ExampleNotFoundException;
import com.uq.exception.UnauthorizedException;
import com.uq.exception.UserNotFoundException;
import com.uq.mapper.EjemploMapper;
import com.uq.model.Ejemplo;
import com.uq.model.Profesor;
import com.uq.repository.EjemploRepository;
import com.uq.repository.ProfesorRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class EjemploService {

    private static final Logger LOGGER = Logger.getLogger(EjemploService.class.getName());

    @Inject
    EjemploRepository ejemploRepository;

    @Inject
    EjemploMapper ejemploMapper;


    @Inject
    ProfesorRepository profesorRepository;

    // Metodo para obtener todos los ejemplos que están marcados como compartidos
    public List<EjemploDTO> listAllSharedExamples() {
        List<Ejemplo> sharedExamples = ejemploRepository.listShared();
        LOGGER.log(Level.INFO, "Obtenidos {0} ejemplos compartidos", sharedExamples.size());
        return ejemploMapper.toDTOList(sharedExamples);
    }

    // Metodo para obtener ejemplos compartidos filtrados por tema
    public List<EjemploDTO> listSharedExamplesByTema(String tema) {
        List<Ejemplo> sharedExamples = ejemploRepository.listSharedByTema(tema);
        LOGGER.log(Level.INFO, "Obtenidos {0} ejemplos compartidos para el tema ''{1}''", new Object[]{sharedExamples.size(), tema});
        return ejemploMapper.toDTOList(sharedExamples);
    }

    // Metodo para obtener un ejemplo compartido por su ID
    public EjemploDTO getSharedExampleById(Long ejemploId) throws ExampleNotFoundException {
        Ejemplo ejemplo = ejemploRepository.findById(ejemploId);

        // Verificar si el ejemplo existe Y si está compartido
        if (ejemplo == null || !ejemplo.isShared()) {
            LOGGER.log(Level.WARNING, "Intento de acceso a ejemplo no compartido o inexistente con ID: {0}", ejemploId);
            throw new ExampleNotFoundException("Ejemplo no encontrado o no compartido.");
        }

        LOGGER.log(Level.INFO, "Acceso autorizado a ejemplo compartido con ID: {0}", ejemploId);
        return ejemploMapper.toDTO(ejemplo);
    }

    // Metodo para crear un ejemplo por parte de un Profesor
    @Transactional
    public EjemploDTO createExampleByProfessor(EjemploDTO ejemploDTO, Long profesorId)
            throws UserNotFoundException, IllegalArgumentException {

        // 1. Validar que los datos básicos del DTO no estén vacíos
        if (ejemploDTO.getTitulo() == null || ejemploDTO.getTitulo().trim().isEmpty() ||
                ejemploDTO.getDescripcion() == null || ejemploDTO.getDescripcion().trim().isEmpty() ||
                ejemploDTO.getCodigoFuente() == null || ejemploDTO.getCodigoFuente().trim().isEmpty() ||
                ejemploDTO.getTema() == null || ejemploDTO.getTema().trim().isEmpty()) {
            throw new IllegalArgumentException("Título, descripción, código fuente y tema son obligatorios.");
        }

        // 2. Validar que el profesor existe
        Profesor profesor = profesorRepository.findById(profesorId);
        if (profesor == null) {
            throw new UserNotFoundException("Profesor que intenta crear ejemplo no encontrado con ID: " + profesorId);
        }
        LOGGER.log(Level.INFO, "Profesor {0} encontrado para crear ejemplo.", profesorId);

        // 3. Mapear el DTO a entidad
        Ejemplo ejemplo = ejemploMapper.toEntity(ejemploDTO);

        // MODIFICACIÓN 1: Asegurar que la propiedad shared está definida correctamente
        // Si utilizamos MapStruct pero queremos asegurarnos que la propiedad se está estableciendo bien
        LOGGER.log(Level.INFO, "Valor de shared en DTO: {0}", ejemploDTO.isShared());
        LOGGER.log(Level.INFO, "Valor de shared en Entity después de mapeo: {0}", ejemplo.isShared());

        // Si necesitas establecerlo explícitamente para asegurar que funciona:
        ejemplo.setShared(ejemploDTO.isShared());

        // 4. Establecer la relación con el profesor
        ejemplo.setProfesor(profesor);

        // 5. Persistir el ejemplo
        ejemploRepository.persist(ejemplo);
        LOGGER.log(Level.INFO, "Ejemplo con ID {0} creado por profesor {1}, compartido: {2}",
                new Object[]{ejemplo.getId(), profesorId, ejemplo.isShared()});

        // 6. Mapear la entidad persistida de vuelta a DTO para la respuesta
        return ejemploMapper.toDTO(ejemplo);
    }

    // Metodo para actualizar un ejemplo completo por parte de un Profesor (PUT)
    @Transactional
    public EjemploDTO updateExample(Long ejemploId, EjemploDTO updatedEjemploDTO, Long authenticatedProfesorId)
            throws ExampleNotFoundException, UnauthorizedException {

        // 1. Buscar el ejemplo existente
        Ejemplo existingEjemplo = ejemploRepository.findById(ejemploId);

        // 2. Verificar si el ejemplo existe
        if (existingEjemplo == null) {
            throw new ExampleNotFoundException("Ejemplo no encontrado con ID: " + ejemploId);
        }

        // 3. Lógica de Autorización: Verificar si el usuario autenticado es el dueño del ejemplo
        if (existingEjemplo.getProfesor() == null || !existingEjemplo.getProfesor().getId().equals(authenticatedProfesorId)) {
            LOGGER.log(Level.WARNING, "Intento de actualización (PUT) no autorizada del ejemplo {0} por profesor {1}", new Object[]{ejemploId, authenticatedProfesorId});
            throw new UnauthorizedException("No tienes permiso para actualizar este ejemplo.");
        }
        LOGGER.log(Level.INFO, "Actualización (PUT) autorizada del ejemplo {0} por profesor {1}", new Object[]{ejemploId, authenticatedProfesorId});


        // 4. Usar el mapper para actualizar la entidad existente desde el DTO completo
        ejemploMapper.updateEntityFromDto(updatedEjemploDTO, existingEjemplo);

        LOGGER.log(Level.INFO, "Ejemplo actualizado (completo) con ID {0} por profesor {1}", new Object[]{ejemploId, authenticatedProfesorId});
        return ejemploMapper.toDTO(existingEjemplo); // Mapear la entidad actualizada de vuelta a DTO
    }


    // Metodo para actualizar un ejemplo parcialmente por parte de un Profesor (PATCH)
    @Transactional
    public EjemploDTO partialUpdateExample(Long ejemploId, EjemploDTO partialEjemploDTO, Long authenticatedProfesorId)
            throws ExampleNotFoundException, UnauthorizedException {

        // 1. Buscar el ejemplo existente
        Ejemplo existingEjemplo = ejemploRepository.findById(ejemploId);

        // 2. Verificar si el ejemplo existe
        if (existingEjemplo == null) {
            throw new ExampleNotFoundException("Ejemplo no encontrado con ID: " + ejemploId);
        }

        // 3. Lógica de Autorización: Verificar si el usuario autenticado es el dueño del ejemplo
        if (existingEjemplo.getProfesor() == null || !existingEjemplo.getProfesor().getId().equals(authenticatedProfesorId)) {
            LOGGER.log(Level.WARNING, "Intento de actualización parcial (PATCH) no autorizada del ejemplo {0} por profesor {1}", new Object[]{ejemploId, authenticatedProfesorId});
            throw new UnauthorizedException("No tienes permiso para actualizar este ejemplo.");
        }
        LOGGER.log(Level.INFO, "Actualización parcial (PATCH) autorizada del ejemplo {0} por profesor {1}", new Object[]{ejemploId, authenticatedProfesorId});


        // 4. Actualizar solo los campos no nulos del DTO parcial
        // PATCH requiere lógica explícita para actualizar solo los campos proporcionados.
        if (partialEjemploDTO.getTitulo() != null) existingEjemplo.setTitulo(partialEjemploDTO.getTitulo());
        if (partialEjemploDTO.getDescripcion() != null) existingEjemplo.setDescripcion(partialEjemploDTO.getDescripcion());
        if (partialEjemploDTO.getCodigoFuente() != null) existingEjemplo.setCodigoFuente(partialEjemploDTO.getCodigoFuente());
        if (partialEjemploDTO.getTema() != null) existingEjemplo.setTema(partialEjemploDTO.getTema());
        if (partialEjemploDTO.getTags() != null) {
            existingEjemplo.setTags(partialEjemploDTO.getTags());
        }
        if (partialEjemploDTO.getDifficulty() != null) {
            existingEjemplo.setDifficulty(partialEjemploDTO.getDifficulty());
        }

        existingEjemplo.setShared(partialEjemploDTO.isShared());

        LOGGER.log(Level.INFO, "Ejemplo actualizado (parcial) con ID {0} por profesor {1}", new Object[]{ejemploId, authenticatedProfesorId});
        return ejemploMapper.toDTO(existingEjemplo); // Mapear la entidad actualizada
    }


    // Metodo para eliminar un ejemplo por parte de un Profesor (DELETE)
    @Transactional
    public void deleteExample(Long ejemploId, Long authenticatedProfesorId)
            throws ExampleNotFoundException, UnauthorizedException {

        // 1. Buscar el ejemplo existente
        Ejemplo existingEjemplo = ejemploRepository.findById(ejemploId);

        // 2. Verificar si el ejemplo existe
        if (existingEjemplo == null) {
            throw new ExampleNotFoundException("Ejemplo no encontrado con ID: " + ejemploId);
        }

        // 3. Lógica de Autorización: Verificar si el usuario autenticado es el dueño del ejemplo
        if (existingEjemplo.getProfesor() == null || !existingEjemplo.getProfesor().getId().equals(authenticatedProfesorId)) {
            LOGGER.log(Level.WARNING, "Intento de eliminación no autorizada del ejemplo {0} por profesor {1}", new Object[]{ejemploId, authenticatedProfesorId});
            throw new UnauthorizedException("No tienes permiso para eliminar este ejemplo.");
        }
        LOGGER.log(Level.INFO, "Eliminación autorizada del ejemplo {0} por profesor {1}", new Object[]{ejemploId, authenticatedProfesorId});


        // 4. Eliminar el ejemplo
        ejemploRepository.delete(existingEjemplo);
    }

}