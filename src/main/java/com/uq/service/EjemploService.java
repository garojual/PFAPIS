package com.uq.service;

import com.uq.dto.EjemploDTO;
import com.uq.exception.ExampleNotFoundException;
import com.uq.mapper.EjemploMapper;
import com.uq.model.Ejemplo;
import com.uq.repository.EjemploRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
}