package es.nonomldev.gestiontunicas.tunica.service;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import es.nonomldev.gestiontunicas.comun.dto.MotivoRequest;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaDTO;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaRequest;
import es.nonomldev.gestiontunicas.tunica.dto.TunicaResumenDTO;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunicaEnum;

/** Inventario de túnicas (Prioste). */
public interface TunicaService {

	/**
	 * Inventario con filtros. {@code estados} vacío = todas menos las que son propiedad de un hermano.
	 * Las medidas filtran por medida total (medida + dobladillo); nulas = sin filtro.
	 */
	Page<TunicaResumenDTO> buscar(Set<EstadoTunicaEnum> estados, Integer medidaMin, Integer medidaMax, Pageable pageable);

	TunicaDTO obtener(Long id);

	/** Alta de una túnica con sus 5 piezas. El código no puede estar en uso en el inventario. */
	TunicaDTO crear(TunicaRequest request);

	TunicaDTO actualizar(Long id, TunicaRequest request);

	TunicaDTO darDeBaja(Long id, MotivoRequest request);

	/** Aviso informativo al dar de alta: el código perteneció a una túnica que ahora es de un hermano. */
	boolean codigoFueDeHermano(String codigo);

	/**
	 * Túnicas libres que encajan con la altura del hermano, ordenadas por cercanía.
	 * {@code toleranciaCm} nula = la del reparto abierto.
	 */
	List<TunicaResumenDTO> proponer(int alturaCm, Integer toleranciaCm);

}
