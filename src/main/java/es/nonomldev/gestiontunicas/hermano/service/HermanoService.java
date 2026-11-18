package es.nonomldev.gestiontunicas.hermano.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import es.nonomldev.gestiontunicas.hermano.dto.ContactoHermanoRequest;
import es.nonomldev.gestiontunicas.hermano.dto.CriterioEdadDTO;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoDTO;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoRequest;
import es.nonomldev.gestiontunicas.hermano.dto.HermanoResumenDTO;

/** Censo de hermanos (Secretaría). */
public interface HermanoService {

	/**
	 * Listado de hermanos: todos, buscando por nombre y apellidos (sin tildes ni mayúsculas, coincidencia parcial).
	 * Con {@code soloEnEdad}, solo los que tienen la edad del reparto a Jueves Santo (ver {@link #criterioEdad()}).
	 */
	Page<HermanoResumenDTO> buscar(String texto, boolean soloEnEdad, Pageable pageable);

	/** Jueves Santo y rango de edad que usa el filtro "En edad" del listado. */
	CriterioEdadDTO criterioEdad();

	HermanoDTO obtener(Long id);

	HermanoDTO crear(HermanoRequest request);

	/** Solo los datos de contacto: los datos del hermano vienen del Excel de Secretaría y no se editan. */
	HermanoDTO actualizarContacto(Long id, ContactoHermanoRequest request);

	/** Baja lógica: no se borra, para conservar su historial. */
	void desactivar(Long id);

}
