package es.nonomldev.gestiontunicas.alquiler.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.alquiler.dto.SituacionHermanoDTO;
import es.nonomldev.gestiontunicas.alquiler.entity.AlquilerEntity;
import es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquilerEnum;
import es.nonomldev.gestiontunicas.alquiler.entity.TipoAlquilerEnum;
import es.nonomldev.gestiontunicas.alquiler.repository.AlquilerRepository;
import es.nonomldev.gestiontunicas.alquiler.service.RequisitosAlquiler;
import es.nonomldev.gestiontunicas.alquiler.service.SituacionHermanoService;
import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.hermano.entity.HermanoEntity;
import es.nonomldev.gestiontunicas.hermano.repository.HermanoRepository;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoRepartoEnum;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoReservaEnum;
import es.nonomldev.gestiontunicas.reparto.entity.RepartoEntity;
import es.nonomldev.gestiontunicas.reparto.entity.ReservaEntity;
import es.nonomldev.gestiontunicas.reparto.repository.RepartoRepository;
import es.nonomldev.gestiontunicas.reparto.repository.ReservaRepository;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunicaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TunicaEntity;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SituacionHermanoServiceImpl implements SituacionHermanoService {

	private final HermanoRepository hermanoRepository;
	private final RepartoRepository repartoRepository;
	private final AlquilerRepository alquilerRepository;
	private final ReservaRepository reservaRepository;
	private final RequisitosAlquiler requisitos;

	@Override
	public SituacionHermanoDTO evaluar(Long hermanoId) {
		HermanoEntity hermano = hermanoRepository.findById(hermanoId)
				.orElseThrow(() -> new RecursoNoEncontradoException("el hermano", hermanoId));
		RepartoEntity reparto = repartoRepository.findFirstByEstado(EstadoRepartoEnum.ABIERTO)
				.orElseThrow(() -> new ReglaNegocioException("No hay ningún reparto abierto"));
		Situacion s = new Situacion(hermano, reparto);

		// 1. Ya tiene túnica en este reparto: solo puede cambiarla
		Optional<AlquilerEntity> activo = alquilerRepository.findFirstByRepartoIdAndHermanoIdAndEstado(
				reparto.getId(), hermanoId, EstadoAlquilerEnum.ACTIVO);
		if (activo.isPresent()) {
			s.puede = true;
			s.tipo = TipoAlquilerEnum.CAMBIO;
			s.alquilerActivo = activo.get();
			return s.toDTO();
		}

		// 2. Requisitos: edad, ficha, veto del año, sanciones pendientes
		Optional<String> motivo = requisitos.motivoNoPuedeAlquilar(hermano, reparto);
		if (motivo.isPresent()) {
			s.motivo = motivo.get();
			return s.toDTO();
		}
		s.puede = true;

		// 3. Renovación: túnica reservada, o derecho a renovar aunque la reserva ya se haya liberado
		Optional<ReservaEntity> reserva = reservaRepository.findByRepartoIdAndHermanoId(reparto.getId(), hermanoId)
				.filter(r -> r.getEstado() == EstadoReservaEnum.ACTIVA);
		if (reserva.isPresent()) {
			s.tipo = TipoAlquilerEnum.RENOVACION;
			s.tunicaReservada = reserva.get().getTunica();
			return s.toDTO();
		}
		Optional<AlquilerEntity> renovacion = requisitos.alquilerQueDaRenovacion(hermanoId, reparto);
		if (renovacion.isPresent()) {
			s.tipo = TipoAlquilerEnum.RENOVACION;
			s.tunicaAnterior = renovacion.get().getTunica();
			return s.toDTO();
		}

		// 4. Nueva alta: se propone primero su antigua túnica si está libre
		s.tipo = TipoAlquilerEnum.NUEVA_ALTA;
		alquilerRepository.findFirstByHermanoIdOrderByFechaEntregaDesc(hermanoId)
				.ifPresent(a -> s.tunicaAnterior = a.getTunica());
		return s.toDTO();
	}

	/** Acumula el resultado de la evaluación. */
	private static final class Situacion {

		private final HermanoEntity hermano;
		private final RepartoEntity reparto;
		private boolean puede;
		private TipoAlquilerEnum tipo;
		private String motivo;
		private AlquilerEntity alquilerActivo;
		private TunicaEntity tunicaReservada;
		private TunicaEntity tunicaAnterior;

		Situacion(HermanoEntity hermano, RepartoEntity reparto) {
			this.hermano = hermano;
			this.reparto = reparto;
		}

		SituacionHermanoDTO toDTO() {
			return new SituacionHermanoDTO(
					hermano.getId(), reparto.getId(), reparto.getAnio(),
					puede, puede ? tipo : null, motivo,
					alquilerActivo == null ? null : alquilerActivo.getId(),
					alquilerActivo == null ? null : alquilerActivo.getTunica().getCodigo(),
					tunicaReservada == null ? null : tunicaReservada.getId(),
					tunicaReservada == null ? null : tunicaReservada.getCodigo(),
					tunicaAnterior == null ? null : tunicaAnterior.getId(),
					tunicaAnterior == null ? null : tunicaAnterior.getCodigo(),
					tunicaAnterior != null && tunicaAnterior.getEstado() == EstadoTunicaEnum.DISPONIBLE,
					hermano.tieneTelefono());
		}

	}

}
