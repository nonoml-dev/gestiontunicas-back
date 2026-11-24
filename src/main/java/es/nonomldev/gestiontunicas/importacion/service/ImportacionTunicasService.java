package es.nonomldev.gestiontunicas.importacion.service;

import java.io.InputStream;
import java.util.List;

import es.nonomldev.gestiontunicas.importacion.dto.DecisionFilaRequest;
import es.nonomldev.gestiontunicas.importacion.dto.ImportacionDTO;
import es.nonomldev.gestiontunicas.importacion.dto.ImportacionFilaDTO;
import es.nonomldev.gestiontunicas.importacion.entity.EstadoFilaEnum;

/**
 * Asistente de importación del inventario de túnicas: subir → revisar → confirmar.
 * Las túnicas se emparejan por código entre las del inventario (las que son de un hermano no cuentan).
 * Nada se guarda en el inventario hasta confirmar.
 */
public interface ImportacionTunicasService {

	/** Lee el Excel y deja la importación PREVISUALIZADA con cada fila clasificada. No toca el inventario. */
	ImportacionDTO previsualizar(String nombreFichero, InputStream excel);

	ImportacionDTO obtener(Long id);

	/** Filas de la importación; {@code estado} nulo = todas. */
	List<ImportacionFilaDTO> filas(Long importacionId, EstadoFilaEnum estado);

	/** Decisión sobre un código que ya está en el inventario: ACTUALIZAR sus medidas o IGNORAR la fila. */
	ImportacionFilaDTO decidir(Long filaId, DecisionFilaRequest request);

	/** Crea las túnicas nuevas, con sus 5 piezas, y actualiza las medidas de las existentes según cada fila. */
	ImportacionDTO confirmar(Long id);

	ImportacionDTO cancelar(Long id);

	/** Excel vacío con la cabecera del inventario (Número · Medida), para descargarlo como plantilla. */
	byte[] plantilla();

}
