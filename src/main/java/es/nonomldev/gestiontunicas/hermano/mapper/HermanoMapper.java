package es.nonomldev.gestiontunicas.hermano.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoDto;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoRequest;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoResumenDto;
import es.nonomldev.gestiontunicas.hermano.dto.TutorDto;
import es.nonomldev.gestiontunicas.hermano.entity.Hermano;
import es.nonomldev.gestiontunicas.hermano.entity.Tutor;

@Mapper(config = MapStructConfig.class)
public interface HermanoMapper {

	@Mapping(target = "nombreCompleto", expression = "java(nombreCompleto(hermano))")
	@Mapping(target = "telefonos", expression = "java(hermano.telefonos())")
	@Mapping(target = "fichaCompleta", expression = "java(hermano.fichaCompleta())")
	HermanoDto toDto(Hermano hermano);

	@Mapping(target = "nombreCompleto", expression = "java(nombreCompleto(hermano))")
	@Mapping(target = "tutorNombre", source = "tutor.nombre")
	@Mapping(target = "fichaCompleta", expression = "java(hermano.fichaCompleta())")
	HermanoResumenDto toResumenDto(Hermano hermano);

	/** El nombre normalizado lo calcula la entidad al guardar; la importación y el estado los gestiona el servicio. */
	@Mapping(target = "nombreNormalizado", ignore = true)
	@Mapping(target = "fechaConsentimiento", ignore = true)
	@Mapping(target = "activo", ignore = true)
	@Mapping(target = "importacion", ignore = true)
	Hermano toEntity(HermanoRequest request);

	@Mapping(target = "nombreNormalizado", ignore = true)
	@Mapping(target = "fechaConsentimiento", ignore = true)
	@Mapping(target = "activo", ignore = true)
	@Mapping(target = "importacion", ignore = true)
	void actualizar(HermanoRequest request, @MappingTarget Hermano hermano);

	TutorDto toDto(Tutor tutor);

	Tutor toEntity(TutorDto dto);

	default String nombreCompleto(Hermano hermano) {
		return hermano.getApellidos() + ", " + hermano.getNombre();
	}

}
