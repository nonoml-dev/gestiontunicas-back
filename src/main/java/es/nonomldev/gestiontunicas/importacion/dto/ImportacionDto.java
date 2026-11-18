package es.nonomldev.gestiontunicas.importacion.dto;

import java.time.Instant;

import es.nonomldev.gestiontunicas.importacion.entity.EstadoImportacion;
import es.nonomldev.gestiontunicas.importacion.entity.TipoImportacion;

/** Resumen de una importación (paso de confirmación del asistente). */
public record ImportacionDto(
		Long id,
		TipoImportacion tipo,
		Long repartoId,
		String nombreFichero,
		EstadoImportacion estado,
		int totalFilas,
		int filasOk,
		int filasError,
		int filasDuplicadas,
		String usuario,
		Instant creadoEn,
		Instant confirmadoEn) {
}
