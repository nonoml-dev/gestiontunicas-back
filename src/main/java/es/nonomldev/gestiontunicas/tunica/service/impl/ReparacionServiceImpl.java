package es.nonomldev.gestiontunicas.tunica.service.impl;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.tunica.dto.ReparacionDTO;
import es.nonomldev.gestiontunicas.tunica.dto.ReparacionRequest;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoPiezaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoReparacionEnum;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunicaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.PiezaEntity;
import es.nonomldev.gestiontunicas.tunica.entity.ReparacionEntity;
import es.nonomldev.gestiontunicas.tunica.entity.TunicaEntity;
import es.nonomldev.gestiontunicas.tunica.mapper.TunicaMapper;
import es.nonomldev.gestiontunicas.tunica.repository.PiezaRepository;
import es.nonomldev.gestiontunicas.tunica.repository.ReparacionRepository;
import es.nonomldev.gestiontunicas.tunica.repository.TunicaRepository;
import es.nonomldev.gestiontunicas.tunica.service.ReparacionService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReparacionServiceImpl implements ReparacionService {

	private static final Set<EstadoReparacionEnum> ABIERTAS = EnumSet.of(EstadoReparacionEnum.PENDIENTE, EstadoReparacionEnum.EN_CURSO);

	/** Estados de la túnica que cambian al abrir o cerrar una reparación (si está fuera, no se toca). */
	private static final Set<EstadoTunicaEnum> EN_CASA = EnumSet.of(EstadoTunicaEnum.DISPONIBLE, EstadoTunicaEnum.EN_REVISION,
			EstadoTunicaEnum.EN_REPARACION, EstadoTunicaEnum.INCOMPLETA);

	private final ReparacionRepository reparacionRepository;
	private final PiezaRepository piezaRepository;
	private final TunicaRepository tunicaRepository;
	private final TunicaMapper mapper;

	@Override
	@Transactional(readOnly = true)
	public List<ReparacionDTO> deTunica(Long tunicaId) {
		return mapper.toReparacionDTOs(reparacionRepository.findByPiezaTunicaIdOrderByFechaInicioDesc(tunicaId));
	}

	@Override
	public ReparacionDTO registrar(Long tunicaId, ReparacionRequest request) {
		TunicaEntity tunica = tunicaRepository.findById(tunicaId)
				.orElseThrow(() -> new RecursoNoEncontradoException("la túnica", tunicaId));
		PiezaEntity pieza = piezaRepository.findByTunicaIdAndTipo(tunicaId, request.tipoPieza())
				.orElseThrow(() -> new RecursoNoEncontradoException(
						"La túnica %s no tiene la pieza %s".formatted(tunica.getCodigo(), request.tipoPieza())));
		validarFechas(request);

		ReparacionEntity reparacion = mapper.toEntity(request);
		reparacion.setPieza(pieza);
		reparacionRepository.save(reparacion);
		aplicarEstado(reparacion, tunica);
		return mapper.toDTO(reparacion);
	}

	@Override
	public ReparacionDTO actualizar(Long id, ReparacionRequest request) {
		ReparacionEntity reparacion = reparacionRepository.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("la reparación", id));
		if (reparacion.getPieza().getTipo() != request.tipoPieza()) {
			throw new ReglaNegocioException("No se puede cambiar la pieza de una reparación ya registrada");
		}
		validarFechas(request);
		mapper.actualizar(request, reparacion);
		aplicarEstado(reparacion, reparacion.getPieza().getTunica());
		return mapper.toDTO(reparacion);
	}

	/**
	 * Reparación abierta: la pieza pasa a "en reparación" y la túnica también.
	 * Reparación finalizada: la pieza queda en buen estado y, si no quedan otras reparaciones abiertas
	 * ni piezas dañadas o perdidas, la túnica vuelve a estar disponible.
	 */
	private void aplicarEstado(ReparacionEntity reparacion, TunicaEntity tunica) {
		PiezaEntity pieza = reparacion.getPieza();
		if (ABIERTAS.contains(reparacion.getEstado())) {
			pieza.setEstado(EstadoPiezaEnum.EN_REPARACION);
			if (EN_CASA.contains(tunica.getEstado())) {
				tunica.setEstado(EstadoTunicaEnum.EN_REPARACION);
			}
			return;
		}
		pieza.setEstado(EstadoPiezaEnum.BUEN_ESTADO);
		reparacionRepository.flush();
		boolean quedanAbiertas = reparacionRepository.existsByPiezaTunicaIdAndEstadoIn(tunica.getId(), ABIERTAS);
		boolean quedanDanadas = piezaRepository.findByTunicaIdOrderByTipo(tunica.getId()).stream()
				.anyMatch(p -> p.getEstado() != EstadoPiezaEnum.BUEN_ESTADO);
		if (!quedanAbiertas && !quedanDanadas && EN_CASA.contains(tunica.getEstado())) {
			tunica.setEstado(EstadoTunicaEnum.DISPONIBLE);
		}
	}

	private static void validarFechas(ReparacionRequest request) {
		if (request.estado() == EstadoReparacionEnum.FINALIZADA && request.fechaFin() == null) {
			throw new ReglaNegocioException("Indica la fecha en que se terminó la reparación");
		}
		if (request.fechaFin() != null && request.fechaFin().isBefore(request.fechaInicio())) {
			throw new ReglaNegocioException("La fecha de fin no puede ser anterior a la de inicio");
		}
	}

}
