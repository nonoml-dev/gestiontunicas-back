package es.nonomldev.gestiontunicas.configuracion.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.configuracion.dto.ConfiguracionDTO;
import es.nonomldev.gestiontunicas.configuracion.entity.ConfiguracionEntity;

@Mapper(config = MapStructConfig.class)
public interface ConfiguracionMapper {

	ConfiguracionDTO toDTO(ConfiguracionEntity configuracion);

	List<ConfiguracionDTO> toDTOs(List<ConfiguracionEntity> configuraciones);

}
