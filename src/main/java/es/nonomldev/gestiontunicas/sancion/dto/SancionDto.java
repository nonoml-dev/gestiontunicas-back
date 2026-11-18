package es.nonomldev.gestiontunicas.sancion.dto;

import java.math.BigDecimal;
import java.time.Instant;

import es.nonomldev.gestiontunicas.sancion.entity.ConceptoSancion;
import es.nonomldev.gestiontunicas.sancion.entity.EstadoSancion;

public record SancionDto(
		Long id,
		Long hermanoId,
		String hermanoNombreCompleto,
		Long repartoId,
		Integer repartoAnio,
		Long alquilerId,
		Long devolucionId,
		ConceptoSancion concepto,
		BigDecimal importeCalculado,
		BigDecimal importe,
		boolean importeAjustado,
		String motivoAjuste,
		EstadoSancion estado,
		Instant fechaResolucion,
		String usuarioResolucion,
		String motivoResolucion,
		Instant creadoEn) {
}
