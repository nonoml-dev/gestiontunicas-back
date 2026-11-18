package es.nonomldev.gestiontunicas.reparto.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Jornada ordinaria (en el asistente del reparto) o extraordinaria.
 * El número y si es extraordinaria los asigna el servicio.
 */
public record JornadaRequest(
		@NotNull LocalDate fecha,
		LocalTime horaInicio,
		LocalTime horaFin,
		/** Solo en extraordinarias. Vacía = la del reparto. */
		@PositiveOrZero @Digits(integer = 6, fraction = 2) BigDecimal limosna,
		boolean permiteRenovacion,
		boolean permiteNuevaAlta,
		boolean permiteCambio) {
}
