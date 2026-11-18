package es.nonomldev.gestiontunicas.tunica.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.tunica.dto.PiezaDTO;
import es.nonomldev.gestiontunicas.tunica.dto.ReparacionDTO;
import es.nonomldev.gestiontunicas.tunica.dto.ReparacionRequest;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaDTO;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaRequest;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaResumenDTO;
import es.nonomldev.gestiontunicas.tunica.entity.PiezaEntity;
import es.nonomldev.gestiontunicas.tunica.entity.ReparacionEntity;
import es.nonomldev.gestiontunicas.tunica.entity.TunicaEntity;

@Mapper(config = MapStructConfig.class)
public interface TunicaMapper {

	@Mapping(target = "monaguillo", expression = "java(tunica.esMonaguillo())")
	TunicaDTO toDTO(TunicaEntity tunica);

	TunicaResumenDTO toResumenDTO(TunicaEntity tunica);

	List<TunicaResumenDTO> toResumenDTOs(List<TunicaEntity> tunicas);

	PiezaDTO toDTO(PiezaEntity pieza);

	/** Estado, piezas, baja y propiedad los gestiona el servicio; la medida total la calcula la BD. */
	@Mapping(target = "medidaTotalCm", ignore = true)
	@Mapping(target = "estado", ignore = true)
	@Mapping(target = "fechaBaja", ignore = true)
	@Mapping(target = "motivoBaja", ignore = true)
	@Mapping(target = "fechaPropiedad", ignore = true)
	@Mapping(target = "importacion", ignore = true)
	@Mapping(target = "piezas", ignore = true)
	TunicaEntity toEntity(TunicaRequest request);

	@Mapping(target = "medidaTotalCm", ignore = true)
	@Mapping(target = "estado", ignore = true)
	@Mapping(target = "fechaAlta", ignore = true)
	@Mapping(target = "fechaBaja", ignore = true)
	@Mapping(target = "motivoBaja", ignore = true)
	@Mapping(target = "fechaPropiedad", ignore = true)
	@Mapping(target = "importacion", ignore = true)
	@Mapping(target = "piezas", ignore = true)
	void actualizar(TunicaRequest request, @MappingTarget TunicaEntity tunica);

	@Mapping(target = "piezaId", source = "pieza.id")
	@Mapping(target = "tipoPieza", source = "pieza.tipo")
	@Mapping(target = "devolucionId", source = "devolucion.id")
	ReparacionDTO toDTO(ReparacionEntity reparacion);

	List<ReparacionDTO> toReparacionDTOs(List<ReparacionEntity> reparaciones);

	/** La pieza (a partir de la túnica y el tipo) y la devolución las asigna el servicio. */
	@Mapping(target = "pieza", ignore = true)
	@Mapping(target = "devolucion", ignore = true)
	@Mapping(target = "estado", defaultValue = "PENDIENTE")
	ReparacionEntity toEntity(ReparacionRequest request);

	@Mapping(target = "pieza", ignore = true)
	@Mapping(target = "devolucion", ignore = true)
	@Mapping(target = "estado", defaultValue = "PENDIENTE")
	void actualizar(ReparacionRequest request, @MappingTarget ReparacionEntity reparacion);

}
