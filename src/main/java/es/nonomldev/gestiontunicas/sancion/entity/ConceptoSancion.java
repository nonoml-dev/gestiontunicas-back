package es.nonomldev.gestiontunicas.sancion.entity;

/** Conceptos de sanción y la letra del importe que se fija al iniciar el reparto. */
public enum ConceptoSancion {
	/** A: túnica, capa o antifaz deteriorada o no entregada (una sola vez). */
	PIEZA_PRINCIPAL,
	/** B: talega no entregada. */
	TALEGA,
	/** D: cíngulo no entregado o deteriorado. */
	CINGULO,
	/** E: devuelta sucia. */
	SUCIEDAD,
	/** C: no devuelta en el plazo estipulado. */
	RETRASO,
	/** F: no devuelta al terminar el plazo de gracia. Se suma a C. */
	CARGO_TOTAL
}
