package es.nonomldev.gestiontunicas.sancion.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import es.nonomldev.gestiontunicas.comun.dto.MotivoRequest;
import es.nonomldev.gestiontunicas.sancion.dto.AjusteSancionRequest;
import es.nonomldev.gestiontunicas.sancion.dto.PagoSancionRequest;
import es.nonomldev.gestiontunicas.sancion.dto.SancionDTO;
import es.nonomldev.gestiontunicas.sancion.entity.ConceptoSancionEnum;
import es.nonomldev.gestiontunicas.sancion.entity.EstadoSancionEnum;

/**
 * Sanciones (Mayordomía). No se borran: se pagan, se condonan o se anulan con motivo.
 * Solo se puede actuar sobre las PENDIENTES.
 */
public interface SancionService {

	/** Filtros opcionales (null = sin filtro). */
	Page<SancionDTO> buscar(EstadoSancionEnum estado, Long repartoId, ConceptoSancionEnum concepto, Pageable pageable);

	List<SancionDTO> deHermano(Long hermanoId);

	SancionDTO ajustar(Long id, AjusteSancionRequest request);

	SancionDTO pagar(Long id, PagoSancionRequest request);

	SancionDTO condonar(Long id, MotivoRequest request);

	SancionDTO anular(Long id, MotivoRequest request);

}
