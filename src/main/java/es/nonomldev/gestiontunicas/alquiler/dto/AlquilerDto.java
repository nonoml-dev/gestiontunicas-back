package es.nonomldev.gestiontunicas.alquiler.dto;

import java.math.BigDecimal;
import java.time.Instant;

import es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquiler;
import es.nonomldev.gestiontunicas.alquiler.entity.TipoAlquiler;

public record AlquilerDto(
		Long id,
		Long repartoId,
		Integer repartoAnio,
		Long hermanoId,
		String hermanoNombreCompleto,
		Long tunicaId,
		String tunicaCodigo,
		Integer tunicaMedidaTotalCm,
		Long jornadaId,
		Integer jornadaNumero,
		TipoAlquiler tipo,
		boolean renovacionDeclarada,
		Long alquilerAnteriorId,
		Long alquilerOrigenId,
		Integer alturaHermanoCm,
		BigDecimal limosna,
		Instant fechaEntrega,
		String usuarioEntrega,
		EstadoAlquiler estado,
		String observaciones) {
}
