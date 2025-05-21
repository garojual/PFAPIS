package com.uq.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor // Constructor sin argumentos
public class ComentarioRequestDTO {
    @NotBlank(message = "El texto del comentario no puede estar vacío")
    private String texto;
}