package es.nonomldev.gestiontunicas.tunica.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.tunica.dto.PiezaDto;
import es.nonomldev.gestiontunicas.tunica.dto.ReparacionDto;
import es.nonomldev.gestiontunicas.tunica.dto.ReparacionRequest;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaDto;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaRequest;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaResumenDto;
import es.nonomldev.gestiontunicas.tunica.entity.Pieza;
import es.nonomldev.gestiontunicas.tunica.entity.Reparacion;
import es.nonomldev.gestiontunicas.tunica.entity.Tunica;

@Mapper(config = MapStructConfig.class)
public interface TunicaMapper {

	@Mapping(target = "monaguillo", expression = "java(tunica.esMonaguillo())")
	TunicaDto toDto(Tunica tunica);

	TunicaResumenDto toResumenDto(Tunica tunica);

	List<TunicaResumenDto> toResumenDtos(List<Tunica> tunicas);

	PiezaDto toDto(Pieza pieza);

	/** Estado, piezas, baja y propiedad los gestiona el servicio; la medida total la calcula la BD. */
	@Mapping(target = "medidaTotalCm", ignore = true)
	@Mapping(target = "estado", ignore = true)
	@Mapping(target = "fechaBaja", ignore = true)
	@Mapping(target = "motivoBaja", ignore = true)
	@Mapping(target = "fechaPropiedad", ignore = true)
	@Mapping(target = "importacion", ignore = true)
	@Mapping(target = "piezas", ignore = true)
	Tunica toEntity(TunicaRequest request);

	@Mapping(target = "medidaTotalCm", ignore = true)
	@Mapping(target = "estado", ignore = true)
	@Mapping(target = "fechaAlta", ignore = true)
	@Mapping(target = "fechaBaja", ignore = true)
	@Mapping(target = "motivoBaja", ignore = true)
	@Mapping(target = "fechaPropiedad", ignore = true)
	@Mapping(target = "importacion", ignore = true)
	@Mapping(target = "piezas", ignore = true)
	void actualizar(TunicaRequest request, @MappingTarget Tunica tunica);

	@Mapping(target = "piezaId", source = "pieza.id")
	@Mapping(target = "tipoPieza", source = "pieza.tipo")
	@Mapping(target = "devolucionId", source = "devolucion.id")
	ReparacionDto toDto(Reparacion reparacion);

	List<ReparacionDto> toReparacionDtos(List<Reparacion> reparaciones);

	/** La pieza (a partir de la túnica y el tipo) y la devolución las asigna el servicio. */
	@Mapping(target = "pieza", ignore = true)
	@Mapping(target = "devolucion", ignore = true)
	@Mapping(target = "estado", defaultValue = "PENDIENTE")
	Reparacion toEntity(ReparacionRequest request);

	@Mapping(target = "pieza", ignore = true)
	@Mapping(target = "devolucion", ignore = true)
	@Mapping(target = "estado", defaultValue = "PENDIENTE")
	void actualizar(ReparacionRequest request, @MappingTarget Reparacion reparacion);

}
