package com.uq.service;

import com.uq.dto.ProgramaDTO; // Necesitas importar ProgramaDTO
import com.uq.dto.UserResponse;
import com.uq.exception.InactiveAccountException;
import com.uq.exception.InvalidCredentialsException;
import com.uq.exception.ProgramExecutionException;
import com.uq.exception.ProgramNotFoundException;
import com.uq.exception.UnauthorizedException;
import com.uq.exception.UserNotFoundException;
import com.uq.mapper.ProfesorMapper;
import com.uq.mapper.ProgramaMapper; // Necesitas importar ProgramaMapper
import com.uq.model.Estudiante;
import com.uq.model.Programa; // Necesitas importar Programa
import com.uq.model.Profesor;
import com.uq.repository.ProfesorRepository;
import com.uq.repository.ProgramaRepository; // Necesitas importar ProgramaRepository
import com.uq.security.JWTUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.mindrot.jbcrypt.BCrypt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;


@ApplicationScoped
public class ProfesorService {

    private static final Logger LOGGER = Logger.getLogger(ProfesorService.class.getName());


    @Inject
    ProfesorRepository profesorRepository;

    @Inject
    ProfesorMapper profesorMapper;

    // Inyectar ProgramaRepository y ProgramaMapper para el nuevo método
    @Inject
    ProgramaRepository programaRepository;

    @Inject
    ProgramaMapper programaMapper;


    public String login(String email, String clave) throws UserNotFoundException, InvalidCredentialsException {
        Optional<Profesor> profesorOptional = profesorRepository.findByEmail(email);

        if (!profesorOptional.isPresent()) {
            throw new UserNotFoundException("Usuario no encontrado.");
        }

        Profesor profesor = profesorOptional.get();

        if (!BCrypt.checkpw(clave, profesor.getContrasena())) {
            throw new InvalidCredentialsException("Credenciales inválidas.");
        }

        return profesor.getEmail();
    }

    @Transactional
    public UserResponse updateProfesor(Long id, Profesor profesor) throws UserNotFoundException, IllegalArgumentException {
        Profesor existingUser = profesorRepository.findById(id);
        if (existingUser == null) {
            throw new UserNotFoundException("Usuario no encontrado.");
        }

        if (profesor.getNombre() != null && !profesor.getNombre().isEmpty()) existingUser.setNombre(profesor.getNombre());
        if (profesor.getEmail() != null && !profesor.getEmail().isEmpty()) {
            Optional<Profesor> existingEmailProf = profesorRepository.findByEmail(profesor.getEmail());
            if (existingEmailProf.isPresent() && !existingEmailProf.get().getId().equals(id)) {
                throw new IllegalArgumentException("El email ya está registrado por otro profesor.");
            }
            existingUser.setEmail(profesor.getEmail());
        }
        if (profesor.getContrasena() != null && !profesor.getContrasena().isEmpty()) {
            existingUser.setContrasena(hashPassword(profesor.getContrasena()));
        }


        return profesorMapper.toResponse(existingUser);
    }

    @Transactional
    public UserResponse partialUpdateProfesor(Long id, Profesor profesor) throws UserNotFoundException, IllegalArgumentException {
        Profesor existingUser = profesorRepository.findById(id);
        if (existingUser == null) {
            throw new UserNotFoundException("Usuario no encontrado.");
        }

        if (profesor.getNombre() != null && !profesor.getNombre().isEmpty()) existingUser.setNombre(profesor.getNombre());
        if (profesor.getEmail() != null && !profesor.getEmail().isEmpty()) {
            Optional<Profesor> existingEmailProf = profesorRepository.findByEmail(profesor.getEmail());
            if (existingEmailProf.isPresent() && !existingEmailProf.get().getId().equals(id)) {
                throw new IllegalArgumentException("El email ya está registrado por otro profesor.");
            }
            existingUser.setEmail(profesor.getEmail());
        }
        if (profesor.getContrasena() != null && !profesor.getContrasena().isEmpty()) {
            existingUser.setContrasena(hashPassword(profesor.getContrasena()));
        }


        return profesorMapper.toResponse(existingUser);
    }

    @Transactional
    public void deleteProfesor(Long id) throws UserNotFoundException { // Renombrado para claridad, profesor delete
        Profesor existingUser = profesorRepository.findById(id);
        if (existingUser == null) {
            throw new UserNotFoundException("Usuario no encontrado.");
        }
        profesorRepository.delete(existingUser);
    }


    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    // Metodo para obtener el ID del profesor por email
    public Long getIdByEmail(String email) throws UserNotFoundException {
        Optional<Profesor> profesorOptional = profesorRepository.findByEmail(email);
        if (!profesorOptional.isPresent()) {
            throw new UserNotFoundException("Usuario no encontrado con email: " + email);
        }
        return profesorOptional.get().getId();
    }

    // Metodo para listar todos los programas
    // Este metodo será llamado por el ProfesorController
    public List<ProgramaDTO> listAllPrograms() {
        List<Programa> programas = programaRepository.listAll();
        LOGGER.log(Level.INFO, "Obtenidos {0} programas (todos) para revisión de profesor", programas.size());
        return programaMapper.toDTOList(programas);
    }
}