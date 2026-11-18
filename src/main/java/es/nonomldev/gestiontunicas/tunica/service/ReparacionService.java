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

	ReparacionDTO registrar(Long tunicaId, ReparacionRequest request);

	/** Al finalizar la última reparación pendiente, la túnica vuelve a estar disponible. */
	ReparacionDTO actualizar(Long id, ReparacionRequest request);

}
