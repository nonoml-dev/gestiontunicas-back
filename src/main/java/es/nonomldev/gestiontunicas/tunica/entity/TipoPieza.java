package es.nonomldev.gestiontunicas.tunica.entity;

/** Las 5 piezas fijas de cada túnica. */
public enum TipoPieza {
	TUNICA,
	CAPA,
	ANTIFAZ,
	CINGULO,
	TALEGA;

	/** Túnica, capa y antifaz: su deterioro o pérdida se cobra con el importe A, una sola vez. */
	public boolean esPrincipal() {
		return this == TUNICA || this == CAPA || this == ANTIFAZ;
	}
}
