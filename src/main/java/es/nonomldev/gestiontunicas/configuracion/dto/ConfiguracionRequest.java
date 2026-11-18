package es.nonomldev.gestiontunicas.configuracion.dto;

/** Nuevo valor de un parámetro. La clave y el tipo no se cambian desde la pantalla. */
public record ConfiguracionRequest(
		String valor) {
}
