package com.uq.mapper;

import com.uq.dto.ProfesorDTO;
import com.uq.dto.UserResponse;
import com.uq.model.Profesor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jakarta")
public interface ProfesorMapper {

    @Mapping(target = "id", ignore = true) // El ID no se mapea, porque lo genera la BD
    Profesor toEntity(ProfesorDTO request);

    UserResponse toResponse(Profesor profesor);
}
