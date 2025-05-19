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
}
