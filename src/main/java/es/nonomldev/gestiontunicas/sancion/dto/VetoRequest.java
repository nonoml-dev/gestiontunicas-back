package es.nonomldev.gestiontunicas.sancion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Veto manual por la gravedad de una incidencia (Administrador o Prioste).
 * Como el automático, dura un solo año: el del reparto indicado.
 */
public record VetoRequest(
		@NotNull Long hermanoId,
		@NotNull @Min(2026) @Max(2100) Integer anioVetado,
		@NotBlank @Size(max = 500) String motivo) {
}
