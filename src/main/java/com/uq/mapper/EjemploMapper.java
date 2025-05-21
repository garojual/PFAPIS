package com.uq.mapper;

import com.uq.dto.EjemploDTO;
import com.uq.model.Ejemplo;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "jakarta",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface EjemploMapper {

    EjemploDTO toDTO(Ejemplo ejemplo);

    // Mapea lista de Entidades a lista de DTOs
    List<EjemploDTO> toDTOList(List<Ejemplo> ejemplos);
}