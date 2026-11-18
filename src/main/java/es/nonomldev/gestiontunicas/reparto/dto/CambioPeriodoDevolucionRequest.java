package es.nonomldev.gestiontunicas.reparto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cambiar el periodo de devolución o el plazo de gracia durante el reparto (queda auditado). */
public record CambioPeriodoDevolucionRequest(
		@NotNull @Valid PeriodoDevolucionRequest periodo,
		@NotBlank @Size(max = 255) String motivo) {
}
