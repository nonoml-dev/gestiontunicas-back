package es.nonomldev.gestiontunicas.reparto.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record JornadaDTO(
		Long id,
		Integer numero,
		boolean extraordinaria,
		LocalDate fecha,
		LocalTime horaInicio,
		LocalTime horaFin,
		/** Solo en extraordinarias. Nula = la del reparto. */
		BigDecimal limosna,
		boolean permiteRenovacion,
		boolean permiteNuevaAlta,
		boolean permiteCambio) {
}
