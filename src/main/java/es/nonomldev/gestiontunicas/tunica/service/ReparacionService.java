package es.nonomldev.gestiontunicas.tunica.service;

import java.util.List;

import es.nonomldev.gestiontunicas.tunica.dto.ReparacionDto;
import es.nonomldev.gestiontunicas.tunica.dto.ReparacionRequest;

/**
 * Reparación o reposición de las piezas de una túnica. Las piezas nunca cambian de túnica:
 * mientras no se repara o repone la pieza, la túnica sigue en reparación o incompleta.
 */
public interface ReparacionService {

	List<ReparacionDto> deTunica(Long tunicaId);

	ReparacionDto registrar(Long tunicaId, ReparacionRequest request);

	/** Al finalizar la última reparación pendiente, la túnica vuelve a estar disponible. */
	ReparacionDto actualizar(Long id, ReparacionRequest request);

}
