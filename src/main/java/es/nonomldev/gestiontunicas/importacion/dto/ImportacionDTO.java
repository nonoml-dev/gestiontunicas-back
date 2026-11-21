package es.nonomldev.gestiontunicas.importacion.dto;

import java.time.Instant;

import es.nonomldev.gestiontunicas.importacion.entity.EstadoImportacionEnum;
import es.nonomldev.gestiontunicas.importacion.entity.TipoImportacionEnum;

/** Resumen de una importación (paso de confirmación del asistente). */
public record ImportacionDTO(
		Long id,
		TipoImportacionEnum tipo,
		Long repartoId,
		String nombreFichero,
		EstadoImportacionEnum estado,
		int totalFilas,
		int filasOk,
		int filasError,
		int filasDuplicadas,
		String usuario,
		Instant creadoEn,
		Instant confirmadoEn) {
}
