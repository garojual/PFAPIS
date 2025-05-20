package com.uq.mapper;

import com.uq.dto.ProgramaDTO;
import com.uq.model.Programa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "jakarta",
        unmappedTargetPolicy = ReportingPolicy.IGNORE // Ignorar campos en DTO que no están en Entidad y viceversa
)
public interface ProgramaMapper {

    // Mapea de DTO a Entidad
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estudiante", ignore = true)
    @Mapping(target = "feedbacks", ignore = true)
    @Mapping(target = "reporte", ignore = true)
    Programa toEntity(ProgramaDTO programaDTO);

    // Mapea de Entidad a DTO
    ProgramaDTO toDTO(Programa programa);

    // Mapea lista de Entidades a lista de DTOs
    List<ProgramaDTO> toDTOList(List<Programa> programas);
}