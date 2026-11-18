package es.nonomldev.gestiontunicas.alquiler.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.alquiler.dto.AlquilerDto;
import es.nonomldev.gestiontunicas.alquiler.dto.CambioTunicaRequest;
import es.nonomldev.gestiontunicas.alquiler.dto.EntregaRequest;
import es.nonomldev.gestiontunicas.alquiler.dto.ResguardoDto;
import es.nonomldev.gestiontunicas.alquiler.dto.SituacionHermanoDto;
import es.nonomldev.gestiontunicas.alquiler.entity.Alquiler;
import es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquiler;
import es.nonomldev.gestiontunicas.alquiler.entity.Resguardo;
import es.nonomldev.gestiontunicas.alquiler.entity.TipoAlquiler;
import es.nonomldev.gestiontunicas.alquiler.mapper.AlquilerMapper;
import es.nonomldev.gestiontunicas.alquiler.repository.AlquilerRepository;
import es.nonomldev.gestiontunicas.alquiler.repository.ResguardoRepository;
import es.nonomldev.gestiontunicas.alquiler.service.AlquilerService;
import es.nonomldev.gestiontunicas.alquiler.service.RequisitosAlquiler;
import es.nonomldev.gestiontunicas.alquiler.service.SituacionHermanoService;
import es.nonomldev.gestiontunicas.comun.dto.MotivoRequest;
import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.devolucion.service.ProcesadorDevolucion;
import es.nonomldev.gestiontunicas.hermano.entity.Hermano;
import es.nonomldev.gestiontunicas.hermano.repository.HermanoRepository;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoReparto;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoReserva;
import es.nonomldev.gestiontunicas.reparto.entity.Jornada;
import es.nonomldev.gestiontunicas.reparto.entity.Reparto;
import es.nonomldev.gestiontunicas.reparto.entity.Reserva;
import es.nonomldev.gestiontunicas.reparto.repository.JornadaRepository;
import es.nonomldev.gestiontunicas.reparto.repository.RepartoRepository;
import es.nonomldev.gestiontunicas.reparto.repository.ReservaRepository;
import es.nonomldev.gestiontunicas.seguridad.entity.Usuario;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunica;
import es.nonomldev.gestiontunicas.tunica.entity.Tunica;
import es.nonomldev.gestiontunicas.tunica.repository.TunicaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AlquilerServiceImpl implements AlquilerService {

	private final AlquilerRepository alquilerRepository;
	private final ResguardoRepository resguardoRepository;
	private final HermanoRepository hermanoRepository;
	private final TunicaRepository tunicaRepository;
	private final RepartoRepository repartoRepository;
	private final JornadaRepository jornadaRepository;
	private final ReservaRepository reservaRepository;
	private final SituacionHermanoService situacionService;
	private final RequisitosAlquiler requisitos;
	private final ProcesadorDevolucion procesadorDevolucion;
	private final AlquilerMapper mapper;
	private final UsuarioActualService usuarioActualService;
	private final Clock clock;

	@Override
	public AlquilerDto entregar(EntregaRequest request) {
		Reparto reparto = repartoAbierto();
		Hermano hermano = hermanoRepository.findById(request.hermanoId())
				.orElseThrow(() -> new RecursoNoEncontradoException("el hermano", request.hermanoId()));

		SituacionHermanoDto situacion = situacionService.evaluar(hermano.getId());
		if (!situacion.puedeAlquilar()) {
			throw new ReglaNegocioException(situacion.motivo());
		}
		if (situacion.tipo() == TipoAlquiler.CAMBIO) {
			throw new ReglaNegocioException("El hermano ya tiene la túnica %s: usa «Cambiar túnica»"
					.formatted(situacion.tunicaActualCodigo()));
		}

		TipoAlquiler tipo = situacion.tipo();
		if (request.renovacionDeclarada()) {
			if (!reparto.isModoTransicion()) {
				throw new ReglaNegocioException("La renovación declarada solo existe en el modo transición 2027");
			}
			tipo = TipoAlquiler.RENOVACION;
		}
		Jornada jornada = jornadaDeHoy(reparto, tipo);

		Tunica tunica = bloquearTunica(request.tunicaId());
		ocuparTunica(tunica, reparto, hermano);

		if (!hermano.tieneTelefono()) {
			if (request.telefono() == null || request.telefono().isBlank()) {
				throw new ReglaNegocioException("Falta un teléfono de contacto: sale en el resguardo");
			}
			hermano.setTelefono(request.telefono().trim());
		}

		Alquiler alquiler = new Alquiler();
		alquiler.setReparto(reparto);
		alquiler.setHermano(hermano);
		alquiler.setTunica(tunica);
		alquiler.setJornada(jornada);
		alquiler.setTipo(tipo);
		alquiler.setRenovacionDeclarada(request.renovacionDeclarada());
		if (tipo == TipoAlquiler.RENOVACION) {
			requisitos.alquilerQueDaRenovacion(hermano.getId(), reparto).ifPresent(alquiler::setAlquilerAnterior);
		}
		alquiler.setAlturaHermanoCm(request.alturaHermanoCm());
		alquiler.setLimosna(jornada.limosnaAplicable(reparto.getLimosna()));
		alquiler.setObservaciones(request.observaciones());
		return mapper.toDto(registrarEntrega(alquiler, tunica));
	}

	@Override
	public AlquilerDto cambiar(Long alquilerId, CambioTunicaRequest request) {
		Alquiler actual = buscar(alquilerId);
		Reparto reparto = repartoAbierto();
		if (!actual.getReparto().getId().equals(reparto.getId()) || actual.getEstado() != EstadoAlquiler.ACTIVO) {
			throw new ReglaNegocioException("Solo se puede cambiar una túnica entregada en el reparto en curso");
		}
		if (actual.getTunica().getId().equals(request.tunicaNuevaId())) {
			throw new ReglaNegocioException("Elige una túnica distinta de la que tiene");
		}
		Jornada jornada = jornadaDeHoy(reparto, TipoAlquiler.CAMBIO);
		Usuario usuario = usuarioActualService.obtener();

		// 1. Devolución rápida de la túnica actual (checklist de las 5 piezas)
		bloquearTunica(actual.getTunica().getId());
		procesadorDevolucion.procesar(actual, request.revisionTunicaActual(), request.observaciones(), true, usuario);
		actual.setEstado(EstadoAlquiler.CAMBIADO);
		actual.resguardoVigente().ifPresent(r -> r.anular("Cambio de túnica"));
		// Hibernate hace los INSERT antes que los UPDATE: sin este flush, el índice
		// "una túnica activa por hermano" vería dos alquileres ACTIVO a la vez.
		alquilerRepository.flush();

		// 2. Entrega de la nueva, con la misma limosna (no se cobra otra vez)
		Tunica nueva = bloquearTunica(request.tunicaNuevaId());
		ocuparTunica(nueva, reparto, actual.getHermano());
		Alquiler cambio = new Alquiler();
		cambio.setReparto(reparto);
		cambio.setHermano(actual.getHermano());
		cambio.setTunica(nueva);
		cambio.setJornada(jornada);
		cambio.setTipo(TipoAlquiler.CAMBIO);
		cambio.setAlquilerOrigen(actual);
		cambio.setAlquilerAnterior(actual.getAlquilerAnterior());
		cambio.setAlturaHermanoCm(request.alturaHermanoCm());
		cambio.setLimosna(actual.getLimosna());
		cambio.setObservaciones(request.observaciones());
		return mapper.toDto(registrarEntrega(cambio, nueva));
	}

	@Override
	@Transactional(readOnly = true)
	public AlquilerDto obtener(Long id) {
		return mapper.toDto(buscar(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<AlquilerDto> historialHermano(Long hermanoId) {
		return mapper.toDtos(alquilerRepository.findByHermanoIdOrderByFechaEntregaDesc(hermanoId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<AlquilerDto> historialTunica(Long tunicaId) {
		return mapper.toDtos(alquilerRepository.findByTunicaIdOrderByFechaEntregaDesc(tunicaId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<AlquilerDto> deReparto(Long repartoId, EstadoAlquiler estado) {
		return mapper.toDtos(alquilerRepository.findByRepartoIdAndEstado(repartoId, estado));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ResguardoDto> resguardos(Long alquilerId) {
		return mapper.toResguardoDtos(resguardoRepository.findByAlquilerIdOrderByFechaEmision(alquilerId));
	}

	@Override
	public ResguardoDto registrarImpresion(Long resguardoId) {
		Resguardo resguardo = buscarResguardo(resguardoId);
		if (resguardo.isAnulado()) {
			throw new ReglaNegocioException("El resguardo %s está anulado".formatted(resguardo.getCodigo()));
		}
		resguardo.registrarImpresion();
		return mapper.toDto(resguardo);
	}

	@Override
	public ResguardoDto anularResguardo(Long resguardoId, MotivoRequest request) {
		Resguardo resguardo = buscarResguardo(resguardoId);
		if (resguardo.isAnulado()) {
			throw new ReglaNegocioException("El resguardo %s ya está anulado".formatted(resguardo.getCodigo()));
		}
		resguardo.anular(request.motivo());
		return mapper.toDto(resguardo);
	}

	/** Guarda la entrega, marca la túnica como alquilada y emite el resguardo con la plantilla vigente. */
	private Alquiler registrarEntrega(Alquiler alquiler, Tunica tunica) {
		Reparto reparto = alquiler.getReparto();
		if (reparto.getPlantillaResguardo() == null) {
			throw new ReglaNegocioException("El reparto no tiene plantilla del resguardo");
		}
		Usuario usuario = usuarioActualService.obtener();
		Instant ahora = Instant.now(clock);
		alquiler.setFechaEntrega(ahora);
		alquiler.setUsuarioEntrega(usuario);
		alquiler.setEstado(EstadoAlquiler.ACTIVO);
		tunica.setEstado(EstadoTunica.ALQUILADA);

		int numero = reparto.siguienteNumeroResguardo();
		Resguardo resguardo = new Resguardo();
		resguardo.setReparto(reparto);
		resguardo.setPlantillaResguardo(reparto.getPlantillaResguardo());
		resguardo.setNumero(numero);
		resguardo.setCodigo(Resguardo.formatearCodigo(reparto.getAnio(), numero));
		resguardo.setFechaEmision(ahora);
		resguardo.setUsuario(usuario);
		alquiler.addResguardo(resguardo);
		return alquilerRepository.save(alquiler);
	}

	/**
	 * La túnica tiene que estar disponible, o reservada para este hermano (renovación). Si el hermano
	 * tenía reservada otra túnica y se lleva esta, su reserva se libera.
	 */
	private void ocuparTunica(Tunica tunica, Reparto reparto, Hermano hermano) {
		Reserva reservaDelHermano = reservaRepository.findByRepartoIdAndHermanoId(reparto.getId(), hermano.getId())
				.filter(r -> r.getEstado() == EstadoReserva.ACTIVA)
				.orElse(null);

		if (tunica.getEstado() == EstadoTunica.RESERVADA) {
			if (reservaDelHermano == null || !reservaDelHermano.getTunica().getId().equals(tunica.getId())) {
				throw new ReglaNegocioException("La túnica %s está reservada para otro hermano".formatted(tunica.getCodigo()));
			}
		} else if (tunica.getEstado() != EstadoTunica.DISPONIBLE) {
			throw new ReglaNegocioException("La túnica %s no está disponible (%s)".formatted(tunica.getCodigo(),
					tunica.getEstado().name().toLowerCase().replace('_', ' ')));
		}

		if (reservaDelHermano != null) {
			Instant ahora = Instant.now(clock);
			reservaDelHermano.setResueltaEn(ahora);
			if (reservaDelHermano.getTunica().getId().equals(tunica.getId())) {
				reservaDelHermano.setEstado(EstadoReserva.RECOGIDA);
			} else {
				reservaDelHermano.setEstado(EstadoReserva.LIBERADA);
				reservaDelHermano.getTunica().setEstado(EstadoTunica.DISPONIBLE);
			}
		}
	}

	/** La operación tiene que estar permitida en la jornada de hoy. */
	private Jornada jornadaDeHoy(Reparto reparto, TipoAlquiler tipo) {
		LocalDate hoy = LocalDate.now(clock);
		Jornada jornada = jornadaRepository.findByRepartoIdAndFecha(reparto.getId(), hoy)
				.orElseThrow(() -> new ReglaNegocioException("Hoy no hay jornada de reparto"));
		if (!jornada.permite(tipo)) {
			throw new ReglaNegocioException("La jornada %d no permite %s".formatted(jornada.getNumero(),
					switch (tipo) {
						case RENOVACION -> "renovaciones";
						case NUEVA_ALTA -> "nuevas altas";
						case CAMBIO -> "cambios";
					}));
		}
		return jornada;
	}

	/** Bloqueo pesimista: los dos puestos no pueden asignar la misma túnica a la vez. */
	private Tunica bloquearTunica(Long tunicaId) {
		return tunicaRepository.findByIdParaActualizar(tunicaId)
				.orElseThrow(() -> new RecursoNoEncontradoException("la túnica", tunicaId));
	}

	private Reparto repartoAbierto() {
		return repartoRepository.findFirstByEstado(EstadoReparto.ABIERTO)
				.orElseThrow(() -> new ReglaNegocioException("No hay ningún reparto abierto"));
	}

	private Alquiler buscar(Long id) {
		return alquilerRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("la entrega", id));
	}

	private Resguardo buscarResguardo(Long id) {
		return resguardoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("el resguardo", id));
	}

}
