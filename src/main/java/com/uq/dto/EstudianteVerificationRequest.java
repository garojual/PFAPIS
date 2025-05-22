package com.uq.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

public class EstudianteVerificationRequest{
        @NotBlank(message = "El correo electrónico no puede estar vacío")
        @Email(message = "Formato de correo electrónico inválido")
        String email;

        @NotBlank(message = "El código de verificación no puede estar vacío")
        String code;

        public String getEmail() {
                return email;
        }

        public void setEmail(String email) {
                this.email = email;
        }

        public String getCode() {
                return code;
        }

        public void setCode(String code) {
                this.code = code;
        }
}