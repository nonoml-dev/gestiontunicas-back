package es.nonomldev.gestiontunicas.importacion.service;

import java.io.InputStream;
import java.util.List;

import es.nonomldev.gestiontunicas.importacion.dto.DecisionFilaRequest;
import es.nonomldev.gestiontunicas.importacion.dto.ImportacionDto;
import es.nonomldev.gestiontunicas.importacion.dto.ImportacionFilaDto;
import es.nonomldev.gestiontunicas.importacion.entity.EstadoFila;

/**
 * Asistente de importación del Excel de hermanos: subir → revisar → confirmar.
 * Los hermanos se emparejan por nombre, apellidos y fecha de nacimiento (el número cambia con los años).
 */
public interface ImportacionHermanosService {

	/** Lee el Excel y deja la importación PREVISUALIZADA con cada fila clasificada. No toca los hermanos. */
	ImportacionDto previsualizar(String nombreFichero, InputStream excel, Long repartoId);

	ImportacionDto obtener(Long id);

	/** Filas de la importación; {@code estado} nulo = todas. */
	List<ImportacionFilaDto> filas(Long importacionId, EstadoFila estado);

	/** Decisión del usuario sobre una fila (sobre todo, los posibles duplicados). */
	ImportacionFilaDto decidir(Long filaId, DecisionFilaRequest request);

	/** Crea y actualiza los hermanos según la decisión de cada fila. */
	ImportacionDto confirmar(Long id);

	ImportacionDto cancelar(Long id);

}
