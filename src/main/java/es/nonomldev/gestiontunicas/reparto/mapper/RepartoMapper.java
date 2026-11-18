package es.nonomldev.gestiontunicas.reparto.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.reparto.dto.CambioPeriodoDevolucionDto;
import es.nonomldev.gestiontunicas.reparto.dto.JornadaDto;
import es.nonomldev.gestiontunicas.reparto.dto.JornadaRequest;
import es.nonomldev.gestiontunicas.reparto.dto.PeriodoDevolucionDto;
import es.nonomldev.gestiontunicas.reparto.dto.PeriodoDevolucionRequest;
import es.nonomldev.gestiontunicas.reparto.dto.PlantillaResguardoDto;
import es.nonomldev.gestiontunicas.reparto.dto.RepartoDto;
import es.nonomldev.gestiontunicas.reparto.dto.RepartoRequest;
import es.nonomldev.gestiontunicas.reparto.dto.ReservaDto;
import es.nonomldev.gestiontunicas.reparto.dto.TarifaSancionesDto;
import es.nonomldev.gestiontunicas.reparto.entity.CambioPeriodoDevolucion;
import es.nonomldev.gestiontunicas.reparto.entity.Jornada;
import es.nonomldev.gestiontunicas.reparto.entity.PeriodoDevolucion;
import es.nonomldev.gestiontunicas.reparto.entity.PlantillaResguardo;
import es.nonomldev.gestiontunicas.reparto.entity.Reparto;
import es.nonomldev.gestiontunicas.reparto.entity.Reserva;
import es.nonomldev.gestiontunicas.reparto.entity.TarifaSanciones;

@Mapper(config = MapStructConfig.class)
public interface RepartoMapper {

	// ---- Reparto ----

	@Mapping(target = "plantillaResguardoId", source = "plantillaResguardo.id")
	@Mapping(target = "plantillaResguardoNombre", source = "plantillaResguardo.nombre")
	RepartoDto toDto(Reparto reparto);

	List<RepartoDto> toDtos(List<Reparto> repartos);

	/**
	 * Estado, fechas de proceso, numeración, plantilla y jornadas los gestiona el servicio.
	 * El Jueves Santo y la marca "manual" también: si viene vacío se calcula.
	 */
	@Mapping(target = "fechaJuevesSanto", ignore = true)
	@Mapping(target = "juevesSantoManual", ignore = true)
	@Mapping(target = "estado", ignore = true)
	@Mapping(target = "plantillaResguardo", ignore = true)
	@Mapping(target = "ultimoNumResguardo", ignore = true)
	@Mapping(target = "fechaApertura", ignore = true)
	@Mapping(target = "reservasLiberadasEn", ignore = true)
	@Mapping(target = "fechaCierre", ignore = true)
	@Mapping(target = "noDevueltasProcesadasEn", ignore = true)
	@Mapping(target = "finGraciaProcesadoEn", ignore = true)
	@Mapping(target = "jornadas", ignore = true)
	Reparto toEntity(RepartoRequest request);

	@Mapping(target = "fechaJuevesSanto", ignore = true)
	@Mapping(target = "juevesSantoManual", ignore = true)
	@Mapping(target = "estado", ignore = true)
	@Mapping(target = "plantillaResguardo", ignore = true)
	@Mapping(target = "ultimoNumResguardo", ignore = true)
	@Mapping(target = "fechaApertura", ignore = true)
	@Mapping(target = "reservasLiberadasEn", ignore = true)
	@Mapping(target = "fechaCierre", ignore = true)
	@Mapping(target = "noDevueltasProcesadasEn", ignore = true)
	@Mapping(target = "finGraciaProcesadoEn", ignore = true)
	@Mapping(target = "jornadas", ignore = true)
	void actualizar(RepartoRequest request, @MappingTarget Reparto reparto);

	// ---- Periodo de devolución y tarifas ----

	@Mapping(target = "finPlazoGracia", expression = "java(periodo.finPlazoGracia())")
	PeriodoDevolucionDto toDto(PeriodoDevolucion periodo);

	PeriodoDevolucion toEntity(PeriodoDevolucionRequest request);

	TarifaSancionesDto toDto(TarifaSanciones tarifas);

	TarifaSanciones toEntity(TarifaSancionesDto dto);

	// ---- Jornadas ----

	JornadaDto toDto(Jornada jornada);

	/** El reparto, el número y si es extraordinaria los asigna el servicio. */
	@Mapping(target = "reparto", ignore = true)
	@Mapping(target = "numero", ignore = true)
	@Mapping(target = "extraordinaria", ignore = true)
	Jornada toEntity(JornadaRequest request);

	// ---- Historial del plazo, reservas y plantilla ----

	@Mapping(target = "usuario", source = "usuario.username")
	CambioPeriodoDevolucionDto toDto(CambioPeriodoDevolucion cambio);

	List<CambioPeriodoDevolucionDto> toCambioDtos(List<CambioPeriodoDevolucion> cambios);

	@Mapping(target = "repartoId", source = "reparto.id")
	@Mapping(target = "hermanoId", source = "hermano.id")
	@Mapping(target = "hermanoNombreCompleto", expression = "java(reserva.getHermano().getApellidos() + \", \" + reserva.getHermano().getNombre())")
	@Mapping(target = "tunicaId", source = "tunica.id")
	@Mapping(target = "tunicaCodigo", source = "tunica.codigo")
	@Mapping(target = "alquilerId", source = "alquiler.id")
	ReservaDto toDto(Reserva reserva);

	List<ReservaDto> toReservaDtos(List<Reserva> reservas);

	@Mapping(target = "subidoPor", source = "subidoPor.username")
	@Mapping(target = "variablesDesconocidas",
			expression = "java(es.nonomldev.gestiontunicas.reparto.service.VariablesResguardo.desconocidas(plantilla.getVariablesDetectadas()))")
	PlantillaResguardoDto toDto(PlantillaResguardo plantilla);

	List<PlantillaResguardoDto> toPlantillaDtos(List<PlantillaResguardo> plantillas);

}
