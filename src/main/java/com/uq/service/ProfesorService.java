package com.uq.service;

import com.uq.dto.UserResponse;
import com.uq.mapper.ProfesorMapper;
import com.uq.model.Profesor;
import com.uq.repository.ProfesorRepository;
import com.uq.security.JWTUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

@ApplicationScoped
public class ProfesorService {

    @Inject
    ProfesorRepository profesorRepository;

    @Inject
    ProfesorMapper profesorMapper;

    public String login(String email, String clave) {
        Optional<Profesor> profesor = profesorRepository.findByEmail(email);
        if (profesor.isPresent() && BCrypt.checkpw(clave, profesor.get().getContrasena())) {
            return JWTUtil.generateToken(profesor.get().getEmail());
        }
        throw new IllegalArgumentException("Credenciales incorrectas.");
    }

    @Transactional
    public UserResponse updateProfesor(Long id, Profesor profesor) {
        Profesor existingUser = profesorRepository.findById(id);
        if (existingUser == null) {
            throw new IllegalArgumentException("Usuario no encontrado.");
        }

        // Actualizar campos
        existingUser.setNombre(profesor.getNombre());
        existingUser.setEmail(profesor.getEmail());
        existingUser.setContrasena(hashPassword(profesor.getContrasena()));

        return profesorMapper.toResponse(existingUser);
    }

    @Transactional
    public UserResponse partialUpdateProfesor(Long id, Profesor profesor) {
        Profesor existingUser = profesorRepository.findById(id);
        if (existingUser == null) {
            throw new IllegalArgumentException("Usuario no encontrado.");
        }

        // Solo actualizar si los valores no son nulos
        if (profesor.getNombre() != null) existingUser.setNombre(profesor.getNombre());
        if (profesor.getEmail() != null) existingUser.setEmail(profesor.getEmail());
        if (profesor.getContrasena() != null) existingUser.setContrasena(hashPassword(profesor.getContrasena()));

        return profesorMapper.toResponse(existingUser);
    }

    @Transactional
    public boolean deleteUser(Long id) {
        return profesorRepository.deleteById(id);
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

}
