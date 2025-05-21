package com.uq.mapper;

import com.uq.dto.EjemploDTO;
import com.uq.model.Ejemplo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "jakarta",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface EjemploMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "profesor", ignore = true)
    @Mapping(source = "shared", target = "shared")
    Ejemplo toEntity(EjemploDTO ejemploDTO);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "profesor.nombre", target = "profesorNombre")
    EjemploDTO toDTO(Ejemplo ejemplo);

    List<EjemploDTO> toDTOList(List<Ejemplo> ejemplos);

    // Metodo para actualizar una entidad existente desde un DTO (para PUT/PATCH)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "profesor", ignore = true)
    @Mapping(source = "shared", target = "shared")
    void updateEntityFromDto(EjemploDTO ejemploDTO, @MappingTarget Ejemplo ejemplo);
}