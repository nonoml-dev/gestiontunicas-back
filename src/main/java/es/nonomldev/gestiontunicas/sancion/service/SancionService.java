package es.nonomldev.gestiontunicas.sancion.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import es.nonomldev.gestiontunicas.comun.dto.MotivoRequest;
import es.nonomldev.gestiontunicas.sancion.dto.AjusteSancionRequest;
import es.nonomldev.gestiontunicas.sancion.dto.PagoSancionRequest;
import es.nonomldev.gestiontunicas.sancion.dto.SancionDto;
import es.nonomldev.gestiontunicas.sancion.entity.ConceptoSancion;
import es.nonomldev.gestiontunicas.sancion.entity.EstadoSancion;

/**
 * Sanciones (Mayordomía). No se borran: se pagan, se condonan o se anulan con motivo.
 * Solo se puede actuar sobre las PENDIENTES.
 */
public interface SancionService {

	/** Filtros opcionales (null = sin filtro). */
	Page<SancionDto> buscar(EstadoSancion estado, Long repartoId, ConceptoSancion concepto, Pageable pageable);

	List<SancionDto> deHermano(Long hermanoId);

	SancionDto ajustar(Long id, AjusteSancionRequest request);

	SancionDto pagar(Long id, PagoSancionRequest request);

	SancionDto condonar(Long id, MotivoRequest request);

	SancionDto anular(Long id, MotivoRequest request);

}
