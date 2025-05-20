package com.uq.service;

import com.uq.dto.EstudianteDTO;
import com.uq.dto.UserResponse;
import com.uq.exception.AccountAlreadyVerifiedException;
import com.uq.exception.ExpiredVerificationCodeException;
import com.uq.exception.InactiveAccountException;
import com.uq.exception.InvalidCredentialsException;
import com.uq.exception.InvalidVerificationCodeException;
import com.uq.exception.UserNotFoundException;
import com.uq.mapper.EstudianteMapper;
import com.uq.model.Estudiante;
import com.uq.repository.EstudianteRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class EstudianteService {

    private static final Logger LOGGER = Logger.getLogger(EstudianteService.class.getName());

    @Inject
    EstudianteRepository estudianteRepository;

    @Inject
    EstudianteMapper estudianteMapper;

    @Inject
    EmailService emailService;

    private static final int VERIFICATION_CODE_VALIDITY_MINUTES = 15;

    @Transactional
    public UserResponse registerEstudiante(EstudianteDTO request) {
        if (request.getEmail() == null || request.getContrasena() == null || request.getNombre() == null) {
            throw new IllegalArgumentException("Todos los campos son obligatorios.");
        }

        if (estudianteRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado.");
        }

        Estudiante estudiante = estudianteMapper.toEntity(request);
        estudiante.setContrasena(hashPassword(request.getContrasena()));

        String verificationCode = generateVerificationCode();
        estudiante.setVerificationCode(verificationCode);
        estudiante.setCodeExpirationDate(LocalDateTime.now().plusMinutes(VERIFICATION_CODE_VALIDITY_MINUTES));
        estudiante.setActive(false);

        estudianteRepository.persist(estudiante);

        try {
            emailService.sendVerificationEmail(estudiante.getEmail(), verificationCode);
            LOGGER.log(Level.INFO, "Solicitud de envío de correo de verificación iniciada para {0}", estudiante.getEmail());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fallo al iniciar el envío del correo de verificación para " + estudiante.getEmail(), e);
        }

        return estudianteMapper.toResponse(estudiante);
    }

    @Transactional
    public void verifyEstudiante(String email, String code)
            throws UserNotFoundException, InvalidVerificationCodeException, ExpiredVerificationCodeException, AccountAlreadyVerifiedException {

        Optional<Estudiante> estudianteOptional = estudianteRepository.findByEmail(email);

        if (!estudianteOptional.isPresent()) {
            throw new UserNotFoundException("Usuario no encontrado.");
        }

        Estudiante estudiante = getEstudianteFromOptional(code, estudianteOptional); // Renombrado el método

        if (estudiante.getCodeExpirationDate() == null || LocalDateTime.now().isAfter(estudiante.getCodeExpirationDate())) {
            throw new ExpiredVerificationCodeException("El código de verificación ha expirado.");
        }

        estudiante.setActive(true);
        estudiante.setVerificationCode(null);
        estudiante.setCodeExpirationDate(null);

    }

    private Estudiante getEstudianteFromOptional(String code, Optional<Estudiante> estudianteOptional) { // Renombrado el método
        Estudiante estudiante = estudianteOptional.get();

        if (estudiante.isActive()) {
            throw new AccountAlreadyVerifiedException("La cuenta ya ha sido verificada previamente.");
        }

        if (estudiante.getVerificationCode() == null || !estudiante.getVerificationCode().equals(code)) {
            throw new InvalidVerificationCodeException("Código de verificación inválido.");
        }
        return estudiante;
    }


    public String login(String email, String clave)
            throws UserNotFoundException, InvalidCredentialsException, InactiveAccountException {

        Optional<Estudiante> estudianteOptional = estudianteRepository.findByEmail(email);

        if (!estudianteOptional.isPresent()) {
            throw new UserNotFoundException("Usuario no encontrado.");
        }

        Estudiante estudiante = estudianteOptional.get();

        if (!estudiante.isActive()) {
            throw new InactiveAccountException("La cuenta no está activa. Por favor, verifica tu correo electrónico.");
        }

        if (!BCrypt.checkpw(clave, estudiante.getContrasena())) {
            throw new InvalidCredentialsException("Credenciales inválidas.");
        }

        return estudiante.getEmail();
    }

    // --- Nuevo método para obtener el ID del estudiante por email ---
    public Long getIdByEmail(String email) throws UserNotFoundException {
        Optional<Estudiante> estudianteOptional = estudianteRepository.findByEmail(email);
        if (!estudianteOptional.isPresent()) {
            throw new UserNotFoundException("Usuario no encontrado con email: " + email);
        }
        return estudianteOptional.get().getId();
    }
    // --- Fin Nuevo método ---


    @Transactional
    public UserResponse updateEstudiante(Long id, Estudiante estudiante) {
        Estudiante existingUser = estudianteRepository.findById(id);
        if (existingUser == null) {
            throw new UserNotFoundException("Usuario no encontrado.");
        }

        if (estudiante.getNombre() != null) existingUser.setNombre(estudiante.getNombre());
        if (estudiante.getEmail() != null) existingUser.setEmail(estudiante.getEmail());
        if (estudiante.getContrasena() != null && !estudiante.getContrasena().isEmpty()) {
            existingUser.setContrasena(hashPassword(estudiante.getContrasena()));
        }

        return estudianteMapper.toResponse(existingUser);
    }

    @Transactional
    public UserResponse partialUpdateUser(Long id, Estudiante estudiante) {
        Estudiante existingUser = estudianteRepository.findById(id);
        if (existingUser == null) {
            throw new UserNotFoundException("Usuario no encontrado.");
        }

        if (estudiante.getNombre() != null && !estudiante.getNombre().isEmpty()) existingUser.setNombre(estudiante.getNombre());
        if (estudiante.getEmail() != null && !estudiante.getEmail().isEmpty()) existingUser.setEmail(estudiante.getEmail());
        if (estudiante.getContrasena() != null && !estudiante.getContrasena().isEmpty()) {
            existingUser.setContrasena(hashPassword(estudiante.getContrasena()));
        }

        return estudianteMapper.toResponse(existingUser);
    }

    @Transactional
    public boolean deleteUser(Long id) {
        Estudiante existingUser = estudianteRepository.findById(id);
        if (existingUser == null) {
            throw new UserNotFoundException("Usuario no encontrado.");
        }
        return estudianteRepository.deleteById(id);
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}