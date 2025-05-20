package com.uq.mapper;

import com.uq.dto.EstudianteDTO;
import com.uq.dto.UserResponse;
import com.uq.model.Estudiante;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jakarta")
public interface EstudianteMapper {

    @Mapping(target = "id", ignore = true) // El ID no se mapea, porque lo genera la BD
    Estudiante toEntity(EstudianteDTO request);

    UserResponse toResponse(Estudiante estudiante);
}
