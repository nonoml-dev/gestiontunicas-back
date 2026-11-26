package es.nonomldev.gestiontunicas.configuracion.dto;

import jakarta.validation.constraints.Size;

/**
 * Nuevo valor de un parámetro. La clave y el tipo no se cambian desde la pantalla. El servicio comprueba que
 * el valor encaje con el tipo del parámetro.
 */
public record ConfiguracionRequest(
		@Size(max = 2000) String valor) {
}
