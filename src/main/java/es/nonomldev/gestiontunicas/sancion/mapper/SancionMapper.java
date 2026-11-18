package es.nonomldev.gestiontunicas.sancion.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.sancion.dto.SancionDto;
import es.nonomldev.gestiontunicas.sancion.dto.VetoDto;
import es.nonomldev.gestiontunicas.sancion.dto.VetoRequest;
import es.nonomldev.gestiontunicas.sancion.entity.Sancion;
import es.nonomldev.gestiontunicas.sancion.entity.Veto;

@Mapper(config = MapStructConfig.class)
public interface SancionMapper {

	@Mapping(target = "hermanoId", source = "hermano.id")
	@Mapping(target = "hermanoNombreCompleto", expression = "java(sancion.getHermano().getApellidos() + \", \" + sancion.getHermano().getNombre())")
	@Mapping(target = "repartoId", source = "reparto.id")
	@Mapping(target = "repartoAnio", source = "reparto.anio")
	@Mapping(target = "alquilerId", source = "alquiler.id")
	@Mapping(target = "devolucionId", source = "devolucion.id")
	@Mapping(target = "importeAjustado", expression = "java(sancion.importeAjustado())")
	@Mapping(target = "usuarioResolucion", source = "usuarioResolucion.username")
	SancionDto toDto(Sancion sancion);

	List<SancionDto> toDtos(List<Sancion> sanciones);

	@Mapping(target = "hermanoId", source = "hermano.id")
	@Mapping(target = "hermanoNombreCompleto", expression = "java(veto.getHermano().getApellidos() + \", \" + veto.getHermano().getNombre())")
	@Mapping(target = "alquilerId", source = "alquiler.id")
	@Mapping(target = "usuario", source = "usuario.username")
	@Mapping(target = "usuarioLevantamiento", source = "usuarioLevantamiento.username")
	VetoDto toDto(Veto veto);

	List<VetoDto> toVetoDtos(List<Veto> vetos);

	/** Veto manual: el hermano, el usuario, la fecha y el origen los asigna el servicio. */
	@Mapping(target = "hermano", ignore = true)
	@Mapping(target = "origen", constant = "MANUAL")
	@Mapping(target = "alquiler", ignore = true)
	@Mapping(target = "fecha", ignore = true)
	@Mapping(target = "usuario", ignore = true)
	@Mapping(target = "activo", constant = "true")
	@Mapping(target = "fechaLevantamiento", ignore = true)
	@Mapping(target = "usuarioLevantamiento", ignore = true)
	@Mapping(target = "motivoLevantamiento", ignore = true)
	Veto toEntity(VetoRequest request);

}
