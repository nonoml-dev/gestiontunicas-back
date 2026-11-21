package es.nonomldev.gestiontunicas.alquiler.dto;

import java.math.BigDecimal;
import java.time.Instant;

import es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquilerEnum;
import es.nonomldev.gestiontunicas.alquiler.entity.TipoAlquilerEnum;

public record AlquilerDTO(
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
		TipoAlquilerEnum tipo,
		boolean renovacionDeclarada,
		Long alquilerAnteriorId,
		Long alquilerOrigenId,
		Integer alturaHermanoCm,
		BigDecimal limosna,
		Instant fechaEntrega,
		String usuarioEntrega,
		EstadoAlquilerEnum estado,
		String observaciones) {
}
