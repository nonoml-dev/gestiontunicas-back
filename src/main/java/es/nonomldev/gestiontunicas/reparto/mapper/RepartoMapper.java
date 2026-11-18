package es.nonomldev.gestiontunicas.reparto.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.reparto.dto.CambioPeriodoDevolucionDTO;
import es.nonomldev.gestiontunicas.reparto.dto.JornadaDTO;
import es.nonomldev.gestiontunicas.reparto.dto.JornadaRequest;
import es.nonomldev.gestiontunicas.reparto.dto.PeriodoDevolucionDTO;
import es.nonomldev.gestiontunicas.reparto.dto.PeriodoDevolucionRequest;
import es.nonomldev.gestiontunicas.reparto.dto.PlantillaResguardoDTO;
import es.nonomldev.gestiontunicas.reparto.dto.RepartoDTO;
import es.nonomldev.gestiontunicas.reparto.dto.RepartoRequest;
import es.nonomldev.gestiontunicas.reparto.dto.ReservaDTO;
import es.nonomldev.gestiontunicas.reparto.dto.TarifaSancionesDTO;
import es.nonomldev.gestiontunicas.reparto.entity.CambioPeriodoDevolucionEntity;
import es.nonomldev.gestiontunicas.reparto.entity.JornadaEntity;
import es.nonomldev.gestiontunicas.reparto.entity.PeriodoDevolucionEntity;
import es.nonomldev.gestiontunicas.reparto.entity.PlantillaResguardoEntity;
import es.nonomldev.gestiontunicas.reparto.entity.RepartoEntity;
import es.nonomldev.gestiontunicas.reparto.entity.ReservaEntity;
import es.nonomldev.gestiontunicas.reparto.entity.TarifaSancionesEntity;

@Mapper(config = MapStructConfig.class)
public interface RepartoMapper {

	// ---- Reparto ----

	@Mapping(target = "plantillaResguardoId", source = "plantillaResguardo.id")
	@Mapping(target = "plantillaResguardoNombre", source = "plantillaResguardo.nombre")
	RepartoDTO toDTO(RepartoEntity reparto);

	List<RepartoDTO> toDTOs(List<RepartoEntity> repartos);

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
	RepartoEntity toEntity(RepartoRequest request);

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
	void actualizar(RepartoRequest request, @MappingTarget RepartoEntity reparto);

	// ---- Periodo de devolución y tarifas ----

	@Mapping(target = "finPlazoGracia", expression = "java(periodo.finPlazoGracia())")
	PeriodoDevolucionDTO toDTO(PeriodoDevolucionEntity periodo);

	PeriodoDevolucionEntity toEntity(PeriodoDevolucionRequest request);

	TarifaSancionesDTO toDTO(TarifaSancionesEntity tarifas);

	TarifaSancionesEntity toEntity(TarifaSancionesDTO dto);

	// ---- Jornadas ----

	JornadaDTO toDTO(JornadaEntity jornada);

	/** El reparto, el número y si es extraordinaria los asigna el servicio. */
	@Mapping(target = "reparto", ignore = true)
	@Mapping(target = "numero", ignore = true)
	@Mapping(target = "extraordinaria", ignore = true)
	JornadaEntity toEntity(JornadaRequest request);

	// ---- Historial del plazo, reservas y plantilla ----

	@Mapping(target = "usuario", source = "usuario.username")
	CambioPeriodoDevolucionDTO toDTO(CambioPeriodoDevolucionEntity cambio);

	List<CambioPeriodoDevolucionDTO> toCambioDTOs(List<CambioPeriodoDevolucionEntity> cambios);

	@Mapping(target = "repartoId", source = "reparto.id")
	@Mapping(target = "hermanoId", source = "hermano.id")
	@Mapping(target = "hermanoNombreCompleto", expression = "java(reserva.getHermano().getApellidos() + \", \" + reserva.getHermano().getNombre())")
	@Mapping(target = "tunicaId", source = "tunica.id")
	@Mapping(target = "tunicaCodigo", source = "tunica.codigo")
	@Mapping(target = "alquilerId", source = "alquiler.id")
	ReservaDTO toDTO(ReservaEntity reserva);

	List<ReservaDTO> toReservaDTOs(List<ReservaEntity> reservas);

	@Mapping(target = "subidoPor", source = "subidoPor.username")
	@Mapping(target = "variablesDesconocidas",
			expression = "java(es.nonomldev.gestiontunicas.reparto.service.VariablesResguardo.desconocidas(plantilla.getVariablesDetectadas()))")
	PlantillaResguardoDTO toDTO(PlantillaResguardoEntity plantilla);

	List<PlantillaResguardoDTO> toPlantillaDTOs(List<PlantillaResguardoEntity> plantillas);

}
