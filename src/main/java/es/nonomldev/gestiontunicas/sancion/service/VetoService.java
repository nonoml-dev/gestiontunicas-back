package es.nonomldev.gestiontunicas.sancion.service;

import java.util.List;

import es.nonomldev.gestiontunicas.comun.dto.MotivoRequest;
import es.nonomldev.gestiontunicas.sancion.dto.VetoDTO;
import es.nonomldev.gestiontunicas.sancion.dto.VetoRequest;

/**
 * Vetos. Todos duran un solo año (C1, Q1). Los automáticos los crea la devolución fuera de plazo;
 * aquí se imponen los manuales (Administrador, Prioste) y se levantan (Administrador, Prioste, Mayordomo).
 */
public interface VetoService {

	List<VetoDTO> deHermano(Long hermanoId);

	/** Vetos activos que impiden alquilar en el reparto de ese año. */
	List<VetoDTO> vigentes(Integer anio);

	VetoDTO imponer(VetoRequest request);

	VetoDTO levantar(Long id, MotivoRequest request);

}
