package es.nonomldev.gestiontunicas.tunica.service;

import java.util.List;

import es.nonomldev.gestiontunicas.tunica.dto.ReparacionDTO;
import es.nonomldev.gestiontunicas.tunica.dto.ReparacionRequest;

/**
 * Reparación o reposición de las piezas de una túnica. Las piezas nunca cambian de túnica:
 * mientras no se repara o repone la pieza, la túnica sigue en reparación o incompleta.
 */
public interface ReparacionService {

	List<ReparacionDTO> deTunica(Long tunicaId);

	/** No se admite en túnicas dadas de baja o que son propiedad de un hermano. */
	ReparacionDTO registrar(Long tunicaId, ReparacionRequest request);

	/**
	 * Cambia el estado, las fechas, la descripción o el coste (la pieza no). Al terminar la última reparación,
	 * la túnica vuelve al estado que corresponde a sus piezas: disponible si están todas bien.
	 */
	ReparacionDTO actualizar(Long tunicaId, Long id, ReparacionRequest request);

}
