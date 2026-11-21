package es.nonomldev.gestiontunicas.alquiler.dto;

import es.nonomldev.gestiontunicas.alquiler.entity.TipoAlquilerEnum;

/**
 * Tarjeta "Reparto" de la ficha del hermano: lo que le toca o el motivo exacto por el que no puede.
 */
public record SituacionHermanoDTO(
		Long hermanoId,
		Long repartoId,
		Integer repartoAnio,
		boolean puedeAlquilar,
		/** RENOVACION, NUEVA_ALTA o CAMBIO (si ya tiene túnica). Nulo si no puede alquilar. */
		TipoAlquilerEnum tipo,
		/** Motivo exacto si no puede alquilar (p. ej. "Sanción pendiente de 15,00 €"). */
		String motivo,
		/** Si ya tiene túnica en este reparto. */
		Long alquilerActivoId,
		String tunicaActualCodigo,
		/** Túnica reservada (renovación). */
		Long tunicaReservadaId,
		String tunicaReservadaCodigo,
		/** Su antigua túnica: se propone primero si está libre. */
		Long tunicaAnteriorId,
		String tunicaAnteriorCodigo,
		boolean tunicaAnteriorDisponible,
		/** Si es false, al entregar hay que pedir un teléfono (sale en el resguardo). */
		boolean tieneTelefono) {
}
