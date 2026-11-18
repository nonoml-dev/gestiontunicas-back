package es.nonomldev.gestiontunicas.alquiler.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.alquiler.dto.AlquilerDTO;
import es.nonomldev.gestiontunicas.alquiler.dto.CambioTunicaRequest;
import es.nonomldev.gestiontunicas.alquiler.dto.EntregaRequest;
import es.nonomldev.gestiontunicas.alquiler.dto.ResguardoDTO;
import es.nonomldev.gestiontunicas.alquiler.dto.SituacionHermanoDTO;
import es.nonomldev.gestiontunicas.alquiler.entity.AlquilerEntity;
import es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquilerEnum;
import es.nonomldev.gestiontunicas.alquiler.entity.ResguardoEntity;
import es.nonomldev.gestiontunicas.alquiler.entity.TipoAlquilerEnum;
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
import es.nonomldev.gestiontunicas.hermano.entity.HermanoEntity;
import es.nonomldev.gestiontunicas.hermano.repository.HermanoRepository;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoRepartoEnum;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoReservaEnum;
import es.nonomldev.gestiontunicas.reparto.entity.JornadaEntity;
import es.nonomldev.gestiontunicas.reparto.entity.RepartoEntity;
import es.nonomldev.gestiontunicas.reparto.entity.ReservaEntity;
import es.nonomldev.gestiontunicas.reparto.repository.JornadaRepository;
import es.nonomldev.gestiontunicas.reparto.repository.RepartoRepository;
import es.nonomldev.gestiontunicas.reparto.repository.ReservaRepository;
import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunicaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TunicaEntity;
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
	public AlquilerDTO entregar(EntregaRequest request) {
		RepartoEntity reparto = repartoAbierto();
		HermanoEntity hermano = hermanoRepository.findById(request.hermanoId())
				.orElseThrow(() -> new RecursoNoEncontradoException("el hermano", request.hermanoId()));

		SituacionHermanoDTO situacion = situacionService.evaluar(hermano.getId());
		if (!situacion.puedeAlquilar()) {
			throw new ReglaNegocioException(situacion.motivo());
		}
		if (situacion.tipo() == TipoAlquilerEnum.CAMBIO) {
			throw new ReglaNegocioException("El hermano ya tiene la túnica %s: usa «Cambiar túnica»"
					.formatted(situacion.tunicaActualCodigo()));
		}

		TipoAlquilerEnum tipo = situacion.tipo();
		if (request.renovacionDeclarada()) {
			if (!reparto.isModoTransicion()) {
				throw new ReglaNegocioException("La renovación declarada solo existe en el modo transición 2027");
			}
			tipo = TipoAlquilerEnum.RENOVACION;
		}
		JornadaEntity jornada = jornadaDeHoy(reparto, tipo);

		TunicaEntity tunica = bloquearTunica(request.tunicaId());
		ocuparTunica(tunica, reparto, hermano);

		if (!hermano.tieneTelefono()) {
			if (request.telefono() == null || request.telefono().isBlank()) {
				throw new ReglaNegocioException("Falta un teléfono de contacto: sale en el resguardo");
			}
			hermano.setTelefono1(request.telefono().trim());
		}

		AlquilerEntity alquiler = new AlquilerEntity();
		alquiler.setReparto(reparto);
		alquiler.setHermano(hermano);
		alquiler.setTunica(tunica);
		alquiler.setJornada(jornada);
		alquiler.setTipo(tipo);
		alquiler.setRenovacionDeclarada(request.renovacionDeclarada());
		if (tipo == TipoAlquilerEnum.RENOVACION) {
			requisitos.alquilerQueDaRenovacion(hermano.getId(), reparto).ifPresent(alquiler::setAlquilerAnterior);
		}
		alquiler.setAlturaHermanoCm(request.alturaHermanoCm());
		alquiler.setLimosna(jornada.limosnaAplicable(reparto.getLimosna()));
		alquiler.setObservaciones(request.observaciones());
		return mapper.toDTO(registrarEntrega(alquiler, tunica));
	}

	@Override
	public AlquilerDTO cambiar(Long alquilerId, CambioTunicaRequest request) {
		AlquilerEntity actual = buscar(alquilerId);
		RepartoEntity reparto = repartoAbierto();
		if (!actual.getReparto().getId().equals(reparto.getId()) || actual.getEstado() != EstadoAlquilerEnum.ACTIVO) {
			throw new ReglaNegocioException("Solo se puede cambiar una túnica entregada en el reparto en curso");
		}
		if (actual.getTunica().getId().equals(request.tunicaNuevaId())) {
			throw new ReglaNegocioException("Elige una túnica distinta de la que tiene");
		}
		JornadaEntity jornada = jornadaDeHoy(reparto, TipoAlquilerEnum.CAMBIO);
		UsuarioEntity usuario = usuarioActualService.obtener();

		// 1. Devolución rápida de la túnica actual (checklist de las 5 piezas)
		bloquearTunica(actual.getTunica().getId());
		procesadorDevolucion.procesar(actual, request.revisionTunicaActual(), request.observaciones(), true, usuario);
		actual.setEstado(EstadoAlquilerEnum.CAMBIADO);
		actual.resguardoVigente().ifPresent(r -> r.anular("Cambio de túnica"));
		// Hibernate hace los INSERT antes que los UPDATE: sin este flush, el índice
		// "una túnica activa por hermano" vería dos alquileres ACTIVO a la vez.
		alquilerRepository.flush();

		// 2. Entrega de la nueva, con la misma limosna (no se cobra otra vez)
		TunicaEntity nueva = bloquearTunica(request.tunicaNuevaId());
		ocuparTunica(nueva, reparto, actual.getHermano());
		AlquilerEntity cambio = new AlquilerEntity();
		cambio.setReparto(reparto);
		cambio.setHermano(actual.getHermano());
		cambio.setTunica(nueva);
		cambio.setJornada(jornada);
		cambio.setTipo(TipoAlquilerEnum.CAMBIO);
		cambio.setAlquilerOrigen(actual);
		cambio.setAlquilerAnterior(actual.getAlquilerAnterior());
		cambio.setAlturaHermanoCm(request.alturaHermanoCm());
		cambio.setLimosna(actual.getLimosna());
		cambio.setObservaciones(request.observaciones());
		return mapper.toDTO(registrarEntrega(cambio, nueva));
	}

	@Override
	@Transactional(readOnly = true)
	public AlquilerDTO obtener(Long id) {
		return mapper.toDTO(buscar(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<AlquilerDTO> historialHermano(Long hermanoId) {
		return mapper.toDTOs(alquilerRepository.findByHermanoIdOrderByFechaEntregaDesc(hermanoId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<AlquilerDTO> historialTunica(Long tunicaId) {
		return mapper.toDTOs(alquilerRepository.findByTunicaIdOrderByFechaEntregaDesc(tunicaId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<AlquilerDTO> deReparto(Long repartoId, EstadoAlquilerEnum estado) {
		return mapper.toDTOs(alquilerRepository.findByRepartoIdAndEstado(repartoId, estado));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ResguardoDTO> resguardos(Long alquilerId) {
		return mapper.toResguardoDTOs(resguardoRepository.findByAlquilerIdOrderByFechaEmision(alquilerId));
	}

	@Override
	public ResguardoDTO registrarImpresion(Long resguardoId) {
		ResguardoEntity resguardo = buscarResguardo(resguardoId);
		if (resguardo.isAnulado()) {
			throw new ReglaNegocioException("El resguardo %s está anulado".formatted(resguardo.getCodigo()));
		}
		resguardo.registrarImpresion();
		return mapper.toDTO(resguardo);
	}

	@Override
	public ResguardoDTO anularResguardo(Long resguardoId, MotivoRequest request) {
		ResguardoEntity resguardo = buscarResguardo(resguardoId);
		if (resguardo.isAnulado()) {
			throw new ReglaNegocioException("El resguardo %s ya está anulado".formatted(resguardo.getCodigo()));
		}
		resguardo.anular(request.motivo());
		return mapper.toDTO(resguardo);
	}

	/** Guarda la entrega, marca la túnica como alquilada y emite el resguardo con la plantilla vigente. */
	private AlquilerEntity registrarEntrega(AlquilerEntity alquiler, TunicaEntity tunica) {
		RepartoEntity reparto = alquiler.getReparto();
		if (reparto.getPlantillaResguardo() == null) {
			throw new ReglaNegocioException("El reparto no tiene plantilla del resguardo");
		}
		UsuarioEntity usuario = usuarioActualService.obtener();
		Instant ahora = Instant.now(clock);
		alquiler.setFechaEntrega(ahora);
		alquiler.setUsuarioEntrega(usuario);
		alquiler.setEstado(EstadoAlquilerEnum.ACTIVO);
		tunica.setEstado(EstadoTunicaEnum.ALQUILADA);

		int numero = reparto.siguienteNumeroResguardo();
		ResguardoEntity resguardo = new ResguardoEntity();
		resguardo.setReparto(reparto);
		resguardo.setPlantillaResguardo(reparto.getPlantillaResguardo());
		resguardo.setNumero(numero);
		resguardo.setCodigo(ResguardoEntity.formatearCodigo(reparto.getAnio(), numero));
		resguardo.setFechaEmision(ahora);
		resguardo.setUsuario(usuario);
		alquiler.addResguardo(resguardo);
		return alquilerRepository.save(alquiler);
	}

	/**
	 * La túnica tiene que estar disponible, o reservada para este hermano (renovación). Si el hermano
	 * tenía reservada otra túnica y se lleva esta, su reserva se libera.
	 */
	private void ocuparTunica(TunicaEntity tunica, RepartoEntity reparto, HermanoEntity hermano) {
		ReservaEntity reservaDelHermano = reservaRepository.findByRepartoIdAndHermanoId(reparto.getId(), hermano.getId())
				.filter(r -> r.getEstado() == EstadoReservaEnum.ACTIVA)
				.orElse(null);

		if (tunica.getEstado() == EstadoTunicaEnum.RESERVADA) {
			if (reservaDelHermano == null || !reservaDelHermano.getTunica().getId().equals(tunica.getId())) {
				throw new ReglaNegocioException("La túnica %s está reservada para otro hermano".formatted(tunica.getCodigo()));
			}
		} else if (tunica.getEstado() != EstadoTunicaEnum.DISPONIBLE) {
			throw new ReglaNegocioException("La túnica %s no está disponible (%s)".formatted(tunica.getCodigo(),
					tunica.getEstado().name().toLowerCase().replace('_', ' ')));
		}

		if (reservaDelHermano != null) {
			Instant ahora = Instant.now(clock);
			reservaDelHermano.setResueltaEn(ahora);
			if (reservaDelHermano.getTunica().getId().equals(tunica.getId())) {
				reservaDelHermano.setEstado(EstadoReservaEnum.RECOGIDA);
			} else {
				reservaDelHermano.setEstado(EstadoReservaEnum.LIBERADA);
				reservaDelHermano.getTunica().setEstado(EstadoTunicaEnum.DISPONIBLE);
			}
		}
	}

	/** La operación tiene que estar permitida en la jornada de hoy. */
	private JornadaEntity jornadaDeHoy(RepartoEntity reparto, TipoAlquilerEnum tipo) {
		LocalDate hoy = LocalDate.now(clock);
		JornadaEntity jornada = jornadaRepository.findByRepartoIdAndFecha(reparto.getId(), hoy)
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
	private TunicaEntity bloquearTunica(Long tunicaId) {
		return tunicaRepository.findByIdParaActualizar(tunicaId)
				.orElseThrow(() -> new RecursoNoEncontradoException("la túnica", tunicaId));
	}

	private RepartoEntity repartoAbierto() {
		return repartoRepository.findFirstByEstado(EstadoRepartoEnum.ABIERTO)
				.orElseThrow(() -> new ReglaNegocioException("No hay ningún reparto abierto"));
	}

	private AlquilerEntity buscar(Long id) {
		return alquilerRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("la entrega", id));
	}

	private ResguardoEntity buscarResguardo(Long id) {
		return resguardoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("el resguardo", id));
	}

}
