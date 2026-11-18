package es.nonomldev.gestiontunicas.notificacion.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.notificacion.dto.NotificacionEmailDto;
import es.nonomldev.gestiontunicas.notificacion.entity.NotificacionEmail;

@Mapper(config = MapStructConfig.class)
public interface NotificacionEmailMapper {

	@Mapping(target = "hermanoId", source = "hermano.id")
	@Mapping(target = "alquilerId", source = "alquiler.id")
	NotificacionEmailDto toDto(NotificacionEmail notificacion);

	List<NotificacionEmailDto> toDtos(List<NotificacionEmail> notificaciones);

}
