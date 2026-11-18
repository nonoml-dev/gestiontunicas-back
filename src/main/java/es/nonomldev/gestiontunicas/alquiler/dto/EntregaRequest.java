package es.nonomldev.gestiontunicas.alquiler.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Diálogo "Entregar túnica". El tipo (renovación o nueva alta), la jornada y la limosna
 * los decide el servicio según la situación del hermano.
 */
public record EntregaRequest(
		@NotNull Long hermanoId,
		@NotNull Long tunicaId,
		/** Medida al entregar. */
		@NotNull @Min(80) @Max(220) Integer alturaHermanoCm,
		/** Obligatorio solo si la ficha no tiene ningún teléfono: sale en el resguardo y se guarda en la ficha. */
		@Size(max = 20) String telefono,
		/** Modo transición 2027: renovación con el código del resguardo en papel de 2026. */
		boolean renovacionDeclarada,
		/** Enviar el resguardo por email (solo si la ficha tiene email). */
		boolean enviarEmail,
		String observaciones) {
}
