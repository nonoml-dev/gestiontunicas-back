package es.nonomldev.gestiontunicas.reparto.dto;

import java.time.Instant;
import java.util.List;

/** Datos de la plantilla Word del resguardo (sin el contenido del fichero). */
public record PlantillaResguardoDTO(
		Long id,
		String nombre,
		String nombreFichero,
		List<String> variablesDetectadas,
		/**
		 * Variables que no están en el catálogo (erratas) o con las llaves mal, tal como están escritas
		 * (p. ej. "{{reparto.importe_d}"). Se avisa al subir la plantilla.
		 */
		List<String> variablesDesconocidas,
		String subidoPor,
		Instant subidoEn) {
}
