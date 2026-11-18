package es.nonomldev.gestiontunicas.reparto.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import es.nonomldev.gestiontunicas.reparto.entity.EstadoReparto;

public record RepartoDto(
		Long id,
		Integer anio,
		LocalDate fechaJuevesSanto,
		boolean juevesSantoManual,
		Integer edadMin,
		Integer edadMax,
		EstadoReparto estado,
		PeriodoDevolucionDto periodoDevolucion,
		String lugarDevolucion,
		BigDecimal limosna,
		TarifaSancionesDto tarifas,
		Integer toleranciaMedidaCm,
		Long plantillaResguardoId,
		String plantillaResguardoNombre,
		boolean modoTransicion,
		Instant fechaApertura,
		Instant reservasLiberadasEn,
		Instant fechaCierre,
		Instant noDevueltasProcesadasEn,
		Instant finGraciaProcesadoEn,
		List<JornadaDto> jornadas) {
}
