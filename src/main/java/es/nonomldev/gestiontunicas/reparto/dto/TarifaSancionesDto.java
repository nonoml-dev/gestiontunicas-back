package es.nonomldev.gestiontunicas.reparto.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Importes de sanción A–F del reparto. Se usa en respuestas y en peticiones. */
public record TarifaSancionesDto(
		/** A: túnica, capa o antifaz deteriorada o no entregada. */
		@NotNull @PositiveOrZero @Digits(integer = 6, fraction = 2) BigDecimal importePiezaPrincipal,
		/** B: talega no entregada. */
		@NotNull @PositiveOrZero @Digits(integer = 6, fraction = 2) BigDecimal importeTalega,
		/** C: no devuelta en plazo. */
		@NotNull @PositiveOrZero @Digits(integer = 6, fraction = 2) BigDecimal importeRetraso,
		/** D: cíngulo no entregado o deteriorado. */
		@NotNull @PositiveOrZero @Digits(integer = 6, fraction = 2) BigDecimal importeCingulo,
		/** E: devuelta sucia. */
		@NotNull @PositiveOrZero @Digits(integer = 6, fraction = 2) BigDecimal importeSuciedad,
		/** F: cargo total de la túnica. */
		@NotNull @PositiveOrZero @Digits(integer = 6, fraction = 2) BigDecimal importeCargoTotal) {
}
