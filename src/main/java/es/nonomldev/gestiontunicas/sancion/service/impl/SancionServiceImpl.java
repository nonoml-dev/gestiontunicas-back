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
import es.nonomldev.gestiontunicas.sancion.dto.SancionDTO;
import es.nonomldev.gestiontunicas.sancion.entity.ConceptoSancionEnum;
import es.nonomldev.gestiontunicas.sancion.entity.EstadoSancionEnum;
import es.nonomldev.gestiontunicas.sancion.entity.SancionEntity;
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
	public Page<SancionDTO> buscar(EstadoSancionEnum estado, Long repartoId, ConceptoSancionEnum concepto, Pageable pageable) {
		return repository.buscar(estado, repartoId, concepto, pageable).map(mapper::toDTO);
	}

	@Override
	@Transactional(readOnly = true)
	public List<SancionDTO> deHermano(Long hermanoId) {
		return mapper.toDTOs(repository.findByHermanoIdOrderByCreadoEnDesc(hermanoId));
	}

	@Override
	public SancionDTO ajustar(Long id, AjusteSancionRequest request) {
		SancionEntity sancion = pendiente(id);
		sancion.setImporte(request.importe());
		sancion.setMotivoAjuste(request.importe().compareTo(sancion.getImporteCalculado()) == 0 ? null : request.motivo());
		return mapper.toDTO(sancion);
	}

	@Override
	public SancionDTO pagar(Long id, PagoSancionRequest request) {
		SancionEntity sancion = pendiente(id);
		Instant fecha = request.fecha() == null || request.fecha().equals(LocalDate.now(clock))
				? Instant.now(clock)
				: request.fecha().atStartOfDay(clock.getZone()).toInstant();
		resolver(sancion, EstadoSancionEnum.PAGADA, request.observaciones(), fecha);
		return mapper.toDTO(sancion);
	}

	@Override
	public SancionDTO condonar(Long id, MotivoRequest request) {
		SancionEntity sancion = pendiente(id);
		resolver(sancion, EstadoSancionEnum.CONDONADA, request.motivo(), Instant.now(clock));
		return mapper.toDTO(sancion);
	}

	@Override
	public SancionDTO anular(Long id, MotivoRequest request) {
		SancionEntity sancion = pendiente(id);
		resolver(sancion, EstadoSancionEnum.ANULADA, request.motivo(), Instant.now(clock));
		return mapper.toDTO(sancion);
	}

	private void resolver(SancionEntity sancion, EstadoSancionEnum estado, String motivo, Instant fecha) {
		sancion.setEstado(estado);
		sancion.setMotivoResolucion(motivo);
		sancion.setFechaResolucion(fecha);
		sancion.setUsuarioResolucion(usuarioActualService.obtener());
	}

	private SancionEntity pendiente(Long id) {
		SancionEntity sancion = repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("la sanción", id));
		if (!sancion.estaPendiente()) {
			throw new ReglaNegocioException("La sanción ya está " + sancion.getEstado().name().toLowerCase());
		}
		return sancion;
	}

}
