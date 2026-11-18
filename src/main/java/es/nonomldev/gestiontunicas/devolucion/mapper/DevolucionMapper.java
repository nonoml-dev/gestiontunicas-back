package es.nonomldev.gestiontunicas.devolucion.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.devolucion.dto.DevolucionDTO;
import es.nonomldev.gestiontunicas.devolucion.dto.RevisionPiezaDTO;
import es.nonomldev.gestiontunicas.devolucion.dto.RevisionPiezaRequest;
import es.nonomldev.gestiontunicas.devolucion.entity.DevolucionEntity;
import es.nonomldev.gestiontunicas.devolucion.entity.RevisionPiezaEntity;

@Mapper(config = MapStructConfig.class)
public interface DevolucionMapper {

	@Mapping(target = "alquilerId", source = "alquiler.id")
	@Mapping(target = "usuario", source = "usuario.username")
	DevolucionDTO toDTO(DevolucionEntity devolucion);

	@Mapping(target = "piezaId", source = "pieza.id")
	@Mapping(target = "tipoPieza", source = "pieza.tipo")
	RevisionPiezaDTO toDTO(RevisionPiezaEntity revision);

	List<RevisionPiezaDTO> toRevisionDTOs(List<RevisionPiezaEntity> revisiones);

	/** La devolución y la pieza (a partir de la túnica y el tipo) las asigna el servicio. */
	@Mapping(target = "devolucion", ignore = true)
	@Mapping(target = "pieza", ignore = true)
	RevisionPiezaEntity toEntity(RevisionPiezaRequest request);

}
