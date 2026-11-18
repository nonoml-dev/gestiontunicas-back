package es.nonomldev.gestiontunicas.sancion.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/** Marcar una sanción como pagada (Mayordomía). Condonar y anular usan MotivoRequest. */
public record PagoSancionRequest(
		/** Nula = hoy. */
		@PastOrPresent LocalDate fecha,
		@Size(max = 255) String observaciones) {
}
