package com.uq.model;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime; // Para la fecha de expiración

@Entity
// @Table(name = "estudiantes") // Si tu tabla no se llama "Estudiante"
public class Estudiante extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String nombre;

    @Column(unique = true)
    public String email;

    public String contrasena;


    public boolean active;

    @Getter
    @Setter
    @Column(name = "verification_code")
    public String verificationCode;

    @Getter
    @Setter
    @Column(name = "code_expiration_date")
    public LocalDateTime codeExpirationDate;

    public Estudiante() {
    }

    // Ejemplo de constructor (sin ID ni campos de verificación)
    public Estudiante(String nombre, String email, String contrasena) {
        this.nombre = nombre;
        this.email = email;
        this.contrasena = contrasena;
        this.active = false; // Por defecto inactivo al crear
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public LocalDateTime getCodeExpirationDate() {
        return codeExpirationDate;
    }

    public void setCodeExpirationDate(LocalDateTime codeExpirationDate) {
        this.codeExpirationDate = codeExpirationDate;
    }
}