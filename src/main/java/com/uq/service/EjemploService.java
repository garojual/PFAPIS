package com.uq.service;

import com.uq.dto.EjemploDTO;
import com.uq.exception.ExampleNotFoundException;
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
}