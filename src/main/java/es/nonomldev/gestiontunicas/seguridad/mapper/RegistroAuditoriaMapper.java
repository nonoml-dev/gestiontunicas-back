package es.nonomldev.gestiontunicas.seguridad.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.seguridad.dto.RegistroAuditoriaDTO;
import es.nonomldev.gestiontunicas.seguridad.entity.RegistroAuditoriaEntity;

@Mapper(config = MapStructConfig.class)
public interface RegistroAuditoriaMapper {

	@Mapping(target = "usuarioId", source = "usuario.id")
	RegistroAuditoriaDTO toDTO(RegistroAuditoriaEntity registro);

}
