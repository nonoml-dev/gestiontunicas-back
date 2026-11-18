package es.nonomldev.gestiontunicas.seguridad.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.seguridad.dto.RegistroAuditoriaDto;
import es.nonomldev.gestiontunicas.seguridad.entity.RegistroAuditoria;

@Mapper(config = MapStructConfig.class)
public interface RegistroAuditoriaMapper {

	@Mapping(target = "usuarioId", source = "usuario.id")
	RegistroAuditoriaDto toDto(RegistroAuditoria registro);

}
