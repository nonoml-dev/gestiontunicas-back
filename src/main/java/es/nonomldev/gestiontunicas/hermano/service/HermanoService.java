package es.nonomldev.gestiontunicas.hermano.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import es.nonomldev.gestiontunicas.hermano.dto.HermanoDto;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoRequest;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoResumenDto;

/** Censo de hermanos (Secretaría). */
public interface HermanoService {

	/**
	 * Buscador por nombre y apellidos (sin tildes ni mayúsculas, coincidencia parcial).
	 * No devuelve a los que ya han cumplido 16 a Jueves Santo: solo se ven en el historial de cada túnica.
	 */
	Page<HermanoResumenDto> buscar(String texto, Pageable pageable);

	HermanoDto obtener(Long id);

	HermanoDto crear(HermanoRequest request);

	HermanoDto actualizar(Long id, HermanoRequest request);

	/** Baja lógica: no se borra, para conservar su historial. */
	void desactivar(Long id);

}
