package com.uq.mapper;

import com.uq.dto.ProgramaDTO;
import com.uq.model.Programa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget; // Importar MappingTarget
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "jakarta",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProgramaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estudiante", ignore = true)
    @Mapping(target = "feedbacks", ignore = true)
    @Mapping(target = "reporte", ignore = true)
    Programa toEntity(ProgramaDTO programaDTO);

    @Mapping(source = "id", target = "id") // Mapea el ID de la Entidad (source) al ID del DTO (target)
    @Mapping(source = "estudiante.id", target = "estudianteId") // Mapea el ID del Estudiante relacionado (source) al nuevo campo del DTO (target)

    ProgramaDTO toDTO(Programa programa);

    List<ProgramaDTO> toDTOList(List<Programa> programas);

    // Metodo para actualizar una entidad existente desde un DTO
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estudiante", ignore = true) // No cambiar el estudiante propietario al actualizar
    @Mapping(target = "feedbacks", ignore = true)
    @Mapping(target = "reporte", ignore = true)
    void updateEntityFromDto(ProgramaDTO programaDTO, @MappingTarget Programa programa);

}