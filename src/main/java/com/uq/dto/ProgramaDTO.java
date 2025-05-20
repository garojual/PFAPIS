package com.uq.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProgramaDTO {
    private String titulo;
    private String descripcion;
    private String codigoFuente;
    private boolean isResuelto;


    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCodigoFuente() {
        return codigoFuente;
    }

    public void setCodigoFuente(String codigoFuente) {
        this.codigoFuente = codigoFuente;
    }

    public boolean isResuelto() {
        return isResuelto;
    }

    public void setResuelto(boolean resuelto) {
        isResuelto = resuelto;
    }
}
