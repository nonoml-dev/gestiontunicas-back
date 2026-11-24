package es.nonomldev.gestiontunicas.tunica.dto;

/**
 * Aviso informativo al dar de alta una túnica: {@code fueDeHermano} indica que el código perteneció a una
 * túnica que ahora es propiedad de un hermano. No impide el alta.
 */
public record AvisoCodigoDTO(
		String codigo,
		boolean fueDeHermano) {
}
