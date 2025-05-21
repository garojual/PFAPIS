package com.uq.mapper;

import com.uq.dto.ComentarioDTO;
import com.uq.model.Comentario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "jakarta",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ComentarioMapper {

    @Mapping(source = "profesor.nombre", target = "profesorNombre")
    ComentarioDTO toDTO(Comentario comentario);

    // Mapea lista de Entidades a lista de DTOs
    List<ComentarioDTO> toDTOList(List<Comentario> comentarios);
}