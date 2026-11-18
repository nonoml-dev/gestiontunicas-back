package es.nonomldev.gestiontunicas.alquiler.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.alquiler.dto.SituacionHermanoDto;
import es.nonomldev.gestiontunicas.alquiler.entity.Alquiler;
import es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquiler;
import es.nonomldev.gestiontunicas.alquiler.entity.TipoAlquiler;
import es.nonomldev.gestiontunicas.alquiler.repository.AlquilerRepository;
import es.nonomldev.gestiontunicas.alquiler.service.RequisitosAlquiler;
import es.nonomldev.gestiontunicas.alquiler.service.SituacionHermanoService;
import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.hermano.entity.Hermano;
import es.nonomldev.gestiontunicas.hermano.repository.HermanoRepository;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoReparto;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoReserva;
import es.nonomldev.gestiontunicas.reparto.entity.Reparto;
import es.nonomldev.gestiontunicas.reparto.entity.Reserva;
import es.nonomldev.gestiontunicas.reparto.repository.RepartoRepository;
import es.nonomldev.gestiontunicas.reparto.repository.ReservaRepository;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunica;
import es.nonomldev.gestiontunicas.tunica.entity.Tunica;
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
	public SituacionHermanoDto evaluar(Long hermanoId) {
		Hermano hermano = hermanoRepository.findById(hermanoId)
				.orElseThrow(() -> new RecursoNoEncontradoException("el hermano", hermanoId));
		Reparto reparto = repartoRepository.findFirstByEstado(EstadoReparto.ABIERTO)
				.orElseThrow(() -> new ReglaNegocioException("No hay ningún reparto abierto"));
		Situacion s = new Situacion(hermano, reparto);

		// 1. Ya tiene túnica en este reparto: solo puede cambiarla
		Optional<Alquiler> activo = alquilerRepository.findFirstByRepartoIdAndHermanoIdAndEstado(
				reparto.getId(), hermanoId, EstadoAlquiler.ACTIVO);
		if (activo.isPresent()) {
			s.puede = true;
			s.tipo = TipoAlquiler.CAMBIO;
			s.alquilerActivo = activo.get();
			return s.toDto();
		}

		// 2. Requisitos: edad, ficha, veto del año, sanciones pendientes
		Optional<String> motivo = requisitos.motivoNoPuedeAlquilar(hermano, reparto);
		if (motivo.isPresent()) {
			s.motivo = motivo.get();
			return s.toDto();
		}
		s.puede = true;

		// 3. Renovación: túnica reservada, o derecho a renovar aunque la reserva ya se haya liberado
		Optional<Reserva> reserva = reservaRepository.findByRepartoIdAndHermanoId(reparto.getId(), hermanoId)
				.filter(r -> r.getEstado() == EstadoReserva.ACTIVA);
		if (reserva.isPresent()) {
			s.tipo = TipoAlquiler.RENOVACION;
			s.tunicaReservada = reserva.get().getTunica();
			return s.toDto();
		}
		Optional<Alquiler> renovacion = requisitos.alquilerQueDaRenovacion(hermanoId, reparto);
		if (renovacion.isPresent()) {
			s.tipo = TipoAlquiler.RENOVACION;
			s.tunicaAnterior = renovacion.get().getTunica();
			return s.toDto();
		}

		// 4. Nueva alta: se propone primero su antigua túnica si está libre
		s.tipo = TipoAlquiler.NUEVA_ALTA;
		alquilerRepository.findFirstByHermanoIdOrderByFechaEntregaDesc(hermanoId)
				.ifPresent(a -> s.tunicaAnterior = a.getTunica());
		return s.toDto();
	}

	/** Acumula el resultado de la evaluación. */
	private static final class Situacion {

		private final Hermano hermano;
		private final Reparto reparto;
		private boolean puede;
		private TipoAlquiler tipo;
		private String motivo;
		private Alquiler alquilerActivo;
		private Tunica tunicaReservada;
		private Tunica tunicaAnterior;

		Situacion(Hermano hermano, Reparto reparto) {
			this.hermano = hermano;
			this.reparto = reparto;
		}

		SituacionHermanoDto toDto() {
			return new SituacionHermanoDto(
					hermano.getId(), reparto.getId(), reparto.getAnio(),
					puede, puede ? tipo : null, motivo,
					alquilerActivo == null ? null : alquilerActivo.getId(),
					alquilerActivo == null ? null : alquilerActivo.getTunica().getCodigo(),
					tunicaReservada == null ? null : tunicaReservada.getId(),
					tunicaReservada == null ? null : tunicaReservada.getCodigo(),
					tunicaAnterior == null ? null : tunicaAnterior.getId(),
					tunicaAnterior == null ? null : tunicaAnterior.getCodigo(),
					tunicaAnterior != null && tunicaAnterior.getEstado() == EstadoTunica.DISPONIBLE,
					hermano.tieneTelefono());
		}

	}

}
