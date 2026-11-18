package es.nonomldev.gestiontunicas.devolucion.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.devolucion.dto.DevolucionDto;
import es.nonomldev.gestiontunicas.devolucion.dto.RevisionPiezaDto;
import es.nonomldev.gestiontunicas.devolucion.dto.RevisionPiezaRequest;
import es.nonomldev.gestiontunicas.devolucion.entity.Devolucion;
import es.nonomldev.gestiontunicas.devolucion.entity.RevisionPieza;

@Mapper(config = MapStructConfig.class)
public interface DevolucionMapper {

	@Mapping(target = "alquilerId", source = "alquiler.id")
	@Mapping(target = "usuario", source = "usuario.username")
	DevolucionDto toDto(Devolucion devolucion);

	@Mapping(target = "piezaId", source = "pieza.id")
	@Mapping(target = "tipoPieza", source = "pieza.tipo")
	RevisionPiezaDto toDto(RevisionPieza revision);

	List<RevisionPiezaDto> toRevisionDtos(List<RevisionPieza> revisiones);

	/** La devolución y la pieza (a partir de la túnica y el tipo) las asigna el servicio. */
	@Mapping(target = "devolucion", ignore = true)
	@Mapping(target = "pieza", ignore = true)
	RevisionPieza toEntity(RevisionPiezaRequest request);

}
