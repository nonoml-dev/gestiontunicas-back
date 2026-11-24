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

	/** Ya no son de la Hermandad o no se van a usar: no se reparan. */
	private static final Set<EstadoTunicaEnum> SIN_REPARACIONES = EnumSet.of(EstadoTunicaEnum.BAJA,
			EstadoTunicaEnum.PROPIEDAD_HERMANO);

	private final ReparacionRepository reparacionRepository;
	private final PiezaRepository piezaRepository;
	private final TunicaRepository tunicaRepository;
	private final TunicaMapper mapper;

	@Override
	@Transactional(readOnly = true)
	public List<ReparacionDTO> deTunica(Long tunicaId) {
		if (!tunicaRepository.existsById(tunicaId)) {
			throw new RecursoNoEncontradoException("la túnica", tunicaId);
		}
		return mapper.toReparacionDTOs(reparacionRepository.findByPiezaTunicaIdOrderByFechaInicioDesc(tunicaId));
	}

	@Override
	public ReparacionDTO registrar(Long tunicaId, ReparacionRequest request) {
		TunicaEntity tunica = tunicaRepository.findById(tunicaId)
				.orElseThrow(() -> new RecursoNoEncontradoException("la túnica", tunicaId));
		if (SIN_REPARACIONES.contains(tunica.getEstado())) {
			throw new ReglaNegocioException("La túnica %s %s: no se le registran reparaciones".formatted(tunica.getCodigo(),
					tunica.getEstado() == EstadoTunicaEnum.BAJA ? "está dada de baja" : "es propiedad de un hermano"));
		}
		PiezaEntity pieza = piezaRepository.findByTunicaIdAndTipo(tunicaId, request.tipoPieza())
				.orElseThrow(() -> new ReglaNegocioException(
						"La túnica %s no tiene la pieza %s".formatted(tunica.getCodigo(), request.tipoPieza())));
		validarFechas(request);

		ReparacionEntity reparacion = mapper.toEntity(request);
		reparacion.setPieza(pieza);
		reparacionRepository.save(reparacion);
		aplicarEstado(reparacion, tunica);
		return mapper.toDTO(reparacion);
	}

	@Override
	public ReparacionDTO actualizar(Long tunicaId, Long id, ReparacionRequest request) {
		ReparacionEntity reparacion = reparacionRepository.findById(id)
				.filter(r -> r.getPieza().getTunica().getId().equals(tunicaId))
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
	 * Reparación abierta: la pieza pasa a "en reparación". Terminada: la pieza vuelve a estar en buen estado
	 * si no tiene otra reparación abierta. Después, si la túnica está en casa, toma el estado que corresponde
	 * a sus piezas (una perdida la deja incompleta; todas bien, disponible).
	 */
	private void aplicarEstado(ReparacionEntity reparacion, TunicaEntity tunica) {
		PiezaEntity pieza = reparacion.getPieza();
		if (ABIERTAS.contains(reparacion.getEstado())) {
			pieza.setEstado(EstadoPiezaEnum.EN_REPARACION);
		} else {
			reparacionRepository.flush();
			boolean otraAbierta = reparacionRepository.existsByPiezaIdAndEstadoIn(pieza.getId(), ABIERTAS);
			pieza.setEstado(otraAbierta ? EstadoPiezaEnum.EN_REPARACION : EstadoPiezaEnum.BUEN_ESTADO);
		}
		if (EN_CASA.contains(tunica.getEstado())) {
			tunica.setEstado(tunica.estadoSegunPiezas());
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
