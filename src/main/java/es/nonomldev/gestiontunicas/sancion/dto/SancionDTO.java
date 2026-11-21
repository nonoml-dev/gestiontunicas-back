package es.nonomldev.gestiontunicas.sancion.dto;

import java.math.BigDecimal;
import java.time.Instant;

import es.nonomldev.gestiontunicas.sancion.entity.ConceptoSancionEnum;
import es.nonomldev.gestiontunicas.sancion.entity.EstadoSancionEnum;

public record SancionDTO(
		Long id,
		Long hermanoId,
		String hermanoNombreCompleto,
		Long repartoId,
		Integer repartoAnio,
		Long alquilerId,
		Long devolucionId,
		ConceptoSancionEnum concepto,
		BigDecimal importeCalculado,
		BigDecimal importe,
		boolean importeAjustado,
		String motivoAjuste,
		EstadoSancionEnum estado,
		Instant fechaResolucion,
		String usuarioResolucion,
		String motivoResolucion,
		Instant creadoEn) {
}
