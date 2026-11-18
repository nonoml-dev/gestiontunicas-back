package es.nonomldev.gestiontunicas.tunica.entity;

public enum EstadoTunicaEnum {
	DISPONIBLE,
	RESERVADA,
	ALQUILADA,
	EN_REVISION,
	EN_REPARACION,
	INCOMPLETA,
	NO_DEVUELTA,
	/** Estado final: no se devolvió en el plazo de gracia. Sale del inventario y libera su código. */
	PROPIEDAD_HERMANO,
	BAJA
}
