package es.nonomldev.gestiontunicas.alquiler.dto;

import java.time.Instant;

public record ResguardoDTO(
		Long id,
		Long alquilerId,
		Long repartoId,
		Long plantillaResguardoId,
		Integer numero,
		/** Número visible, p. ej. "2027-0042". */
		String codigo,
		Instant fechaEmision,
		String usuario,
		Integer numImpresiones,
		boolean anulado,
		Instant fechaAnulacion,
		String motivoAnulacion) {
}
