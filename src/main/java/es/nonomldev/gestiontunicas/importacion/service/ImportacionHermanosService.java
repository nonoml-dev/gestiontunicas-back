package es.nonomldev.gestiontunicas.importacion.service;

import java.io.InputStream;
import java.util.List;

import es.nonomldev.gestiontunicas.importacion.dto.DecisionFilaRequest;
import es.nonomldev.gestiontunicas.importacion.dto.ImportacionDTO;
import es.nonomldev.gestiontunicas.importacion.dto.ImportacionFilaDTO;
import es.nonomldev.gestiontunicas.importacion.entity.EstadoFilaEnum;

/**
 * Asistente de importación del Excel de hermanos: subir → revisar → confirmar.
 * Los hermanos se emparejan por nombre, apellidos y fecha de nacimiento (el número cambia con los años).
 */
public interface ImportacionHermanosService {

	/** Lee el Excel y deja la importación PREVISUALIZADA con cada fila clasificada. No toca los hermanos. */
	ImportacionDTO previsualizar(String nombreFichero, InputStream excel, Long repartoId);

	ImportacionDTO obtener(Long id);

	/** Filas de la importación; {@code estado} nulo = todas. */
	List<ImportacionFilaDTO> filas(Long importacionId, EstadoFilaEnum estado);

	/** Decisión del usuario sobre una fila (sobre todo, los posibles duplicados). */
	ImportacionFilaDTO decidir(Long filaId, DecisionFilaRequest request);

	/** Crea y actualiza los hermanos según la decisión de cada fila. */
	ImportacionDTO confirmar(Long id);

	ImportacionDTO cancelar(Long id);

	/** Excel vacío con la cabecera del formato de Secretaría, para descargarlo como plantilla. */
	byte[] plantilla();

}
