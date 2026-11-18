package es.nonomldev.gestiontunicas.configuracion.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.configuracion.dto.ConfiguracionDto;
import es.nonomldev.gestiontunicas.configuracion.dto.ConfiguracionRequest;
import es.nonomldev.gestiontunicas.configuracion.entity.Configuracion;

@Mapper(config = MapStructConfig.class)
public interface ConfiguracionMapper {

	ConfiguracionDto toDto(Configuracion configuracion);

	List<ConfiguracionDto> toDtos(List<Configuracion> configuraciones);

	@Mapping(target = "clave", ignore = true)
	@Mapping(target = "tipo", ignore = true)
	@Mapping(target = "descripcion", ignore = true)
	void actualizar(ConfiguracionRequest request, @MappingTarget Configuracion configuracion);

}
