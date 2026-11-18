package es.nonomldev.gestiontunicas.configuracion.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.configuracion.dto.ConfiguracionDTO;
import es.nonomldev.gestiontunicas.configuracion.dto.ConfiguracionRequest;
import es.nonomldev.gestiontunicas.configuracion.entity.ConfiguracionEntity;

@Mapper(config = MapStructConfig.class)
public interface ConfiguracionMapper {

	ConfiguracionDTO toDTO(ConfiguracionEntity configuracion);

	List<ConfiguracionDTO> toDTOs(List<ConfiguracionEntity> configuraciones);

	@Mapping(target = "clave", ignore = true)
	@Mapping(target = "tipo", ignore = true)
	@Mapping(target = "descripcion", ignore = true)
	void actualizar(ConfiguracionRequest request, @MappingTarget ConfiguracionEntity configuracion);

}
