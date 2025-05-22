package com.uq.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfesorLoginDTO {
    private String email;
    private String contrasena;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
}
