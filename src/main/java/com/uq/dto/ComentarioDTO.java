package com.uq.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ComentarioDTO {
    private Long id;
    private String texto;
    private LocalDateTime fecha;
    private String profesorNombre;
}