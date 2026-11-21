package es.nonomldev.gestiontunicas.reparto.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import es.nonomldev.gestiontunicas.reparto.entity.EstadoRepartoEnum;

public record RepartoDTO(
		Long id,
		Integer anio,
		LocalDate fechaJuevesSanto,
		boolean juevesSantoManual,
		Integer edadMin,
		Integer edadMax,
		EstadoRepartoEnum estado,
		PeriodoDevolucionDTO periodoDevolucion,
		String lugarDevolucion,
		BigDecimal limosna,
		TarifaSancionesDTO tarifas,
		Integer toleranciaMedidaCm,
		Long plantillaResguardoId,
		String plantillaResguardoNombre,
		boolean modoTransicion,
		Instant fechaApertura,
		Instant reservasLiberadasEn,
		Instant fechaCierre,
		Instant noDevueltasProcesadasEn,
		Instant finGraciaProcesadoEn,
		List<JornadaDTO> jornadas) {
}
