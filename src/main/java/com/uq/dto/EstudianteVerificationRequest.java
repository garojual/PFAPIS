package com.uq.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

public record EstudianteVerificationRequest(
        @Getter
        @Setter
        @NotBlank(message = "El correo electrónico no puede estar vacío")
        @Email(message = "Formato de correo electrónico inválido")
        String email,

        @Getter
        @Setter
        @NotBlank(message = "El código de verificación no puede estar vacío")
        String code
) {}