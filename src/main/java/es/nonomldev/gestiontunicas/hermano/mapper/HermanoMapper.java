package es.nonomldev.gestiontunicas.hermano.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoDTO;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoRequest;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoResumenDTO;
import es.nonomldev.gestiontunicas.hermano.entity.HermanoEntity;

@Mapper(config = MapStructConfig.class)
public interface HermanoMapper {

	@Mapping(target = "nombreCompleto", expression = "java(nombreCompleto(hermano))")
	@Mapping(target = "fichaCompleta", expression = "java(hermano.fichaCompleta())")
	HermanoDTO toDTO(HermanoEntity hermano);

	@Mapping(target = "nombreCompleto", expression = "java(nombreCompleto(hermano))")
	@Mapping(target = "fichaCompleta", expression = "java(hermano.fichaCompleta())")
	HermanoResumenDTO toResumenDTO(HermanoEntity hermano);

	/** El nombre normalizado lo calcula la entidad al guardar; la importación y el estado los gestiona el servicio. */
	@Mapping(target = "nombreNormalizado", ignore = true)
	@Mapping(target = "numeroOrden", ignore = true)
	@Mapping(target = "activo", ignore = true)
	@Mapping(target = "importacion", ignore = true)
	HermanoEntity toEntity(HermanoRequest request);

	default String nombreCompleto(HermanoEntity hermano) {
		return hermano.getApellidos() + ", " + hermano.getNombre();
	}

}
