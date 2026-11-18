package es.nonomldev.gestiontunicas.sancion.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.comun.dto.MotivoRequest;
import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.sancion.dto.AjusteSancionRequest;
import es.nonomldev.gestiontunicas.sancion.dto.PagoSancionRequest;
import es.nonomldev.gestiontunicas.sancion.dto.SancionDto;
import es.nonomldev.gestiontunicas.sancion.entity.ConceptoSancion;
import es.nonomldev.gestiontunicas.sancion.entity.EstadoSancion;
import es.nonomldev.gestiontunicas.sancion.entity.Sancion;
import es.nonomldev.gestiontunicas.sancion.mapper.SancionMapper;
import es.nonomldev.gestiontunicas.sancion.repository.SancionRepository;
import es.nonomldev.gestiontunicas.sancion.service.SancionService;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SancionServiceImpl implements SancionService {

	private final SancionRepository repository;
	private final SancionMapper mapper;
	private final UsuarioActualService usuarioActualService;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public Page<SancionDto> buscar(EstadoSancion estado, Long repartoId, ConceptoSancion concepto, Pageable pageable) {
		return repository.buscar(estado, repartoId, concepto, pageable).map(mapper::toDto);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SancionDto> deHermano(Long hermanoId) {
		return mapper.toDtos(repository.findByHermanoIdOrderByCreadoEnDesc(hermanoId));
	}

	@Override
	public SancionDto ajustar(Long id, AjusteSancionRequest request) {
		Sancion sancion = pendiente(id);
		sancion.setImporte(request.importe());
		sancion.setMotivoAjuste(request.importe().compareTo(sancion.getImporteCalculado()) == 0 ? null : request.motivo());
		return mapper.toDto(sancion);
	}

	@Override
	public SancionDto pagar(Long id, PagoSancionRequest request) {
		Sancion sancion = pendiente(id);
		Instant fecha = request.fecha() == null || request.fecha().equals(LocalDate.now(clock))
				? Instant.now(clock)
				: request.fecha().atStartOfDay(clock.getZone()).toInstant();
		resolver(sancion, EstadoSancion.PAGADA, request.observaciones(), fecha);
		return mapper.toDto(sancion);
	}

	@Override
	public SancionDto condonar(Long id, MotivoRequest request) {
		Sancion sancion = pendiente(id);
		resolver(sancion, EstadoSancion.CONDONADA, request.motivo(), Instant.now(clock));
		return mapper.toDto(sancion);
	}

	@Override
	public SancionDto anular(Long id, MotivoRequest request) {
		Sancion sancion = pendiente(id);
		resolver(sancion, EstadoSancion.ANULADA, request.motivo(), Instant.now(clock));
		return mapper.toDto(sancion);
	}

	private void resolver(Sancion sancion, EstadoSancion estado, String motivo, Instant fecha) {
		sancion.setEstado(estado);
		sancion.setMotivoResolucion(motivo);
		sancion.setFechaResolucion(fecha);
		sancion.setUsuarioResolucion(usuarioActualService.obtener());
	}

	private Sancion pendiente(Long id) {
		Sancion sancion = repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("la sanción", id));
		if (!sancion.estaPendiente()) {
			throw new ReglaNegocioException("La sanción ya está " + sancion.getEstado().name().toLowerCase());
		}
		return sancion;
	}

}
