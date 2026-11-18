package es.nonomldev.gestiontunicas.alquiler.entity;

public enum EstadoAlquiler {
	ACTIVO,
	CAMBIADO,
	DEVUELTO,
	NO_DEVUELTO,
	/** No se devolvió en el plazo de gracia: cargo total y la túnica pasa a ser del hermano. */
	ADQUIRIDA_POR_HERMANO
}
