package es.nonomldev.gestiontunicas.comun.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Petición que solo lleva un motivo obligatorio: condonar o anular una sanción, levantar un veto,
 * anular un resguardo, dar de baja una túnica...
 */
public record MotivoRequest(
		@NotBlank @Size(max = 500) String motivo) {
}
