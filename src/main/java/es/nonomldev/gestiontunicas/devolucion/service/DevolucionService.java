package es.nonomldev.gestiontunicas.devolucion.service;

import es.nonomldev.gestiontunicas.devolucion.dto.DevolucionDTO;
import es.nonomldev.gestiontunicas.devolucion.dto.DevolucionRequest;

/** Devolución de túnicas con revisión pieza a pieza (Prioste y Capiller). */
public interface DevolucionService {

	/**
	 * Registra la devolución: estado de piezas y túnica, sanciones calculadas y, si es fuera de plazo,
	 * sanción C y veto para el año próximo. Se admite también durante el plazo de gracia.
	 */
	DevolucionDTO registrar(DevolucionRequest request);

	DevolucionDTO obtenerPorAlquiler(Long alquilerId);

}
