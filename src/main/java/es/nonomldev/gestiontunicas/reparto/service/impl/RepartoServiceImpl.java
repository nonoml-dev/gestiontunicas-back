package es.nonomldev.gestiontunicas.reparto.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.alquiler.entity.Alquiler;
import es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquiler;
import es.nonomldev.gestiontunicas.alquiler.repository.AlquilerRepository;
import es.nonomldev.gestiontunicas.alquiler.service.RequisitosAlquiler;
import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.hermano.entity.Hermano;
import es.nonomldev.gestiontunicas.reparto.dto.CambioPeriodoDevolucionDto;
import es.nonomldev.gestiontunicas.reparto.dto.CambioPeriodoDevolucionRequest;
import es.nonomldev.gestiontunicas.reparto.dto.JornadaRequest;
import es.nonomldev.gestiontunicas.reparto.dto.RepartoDto;
import es.nonomldev.gestiontunicas.reparto.dto.RepartoRequest;
import es.nonomldev.gestiontunicas.reparto.dto.ReservaDto;
import es.nonomldev.gestiontunicas.reparto.entity.CambioPeriodoDevolucion;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoReparto;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoReserva;
import es.nonomldev.gestiontunicas.reparto.entity.Jornada;
import es.nonomldev.gestiontunicas.reparto.entity.PeriodoDevolucion;
import es.nonomldev.gestiontunicas.reparto.entity.Reparto;
import es.nonomldev.gestiontunicas.reparto.entity.Reserva;
import es.nonomldev.gestiontunicas.reparto.mapper.RepartoMapper;
import es.nonomldev.gestiontunicas.reparto.repository.CambioPeriodoDevolucionRepository;
import es.nonomldev.gestiontunicas.reparto.repository.JornadaRepository;
import es.nonomldev.gestiontunicas.reparto.repository.PlantillaResguardoRepository;
import es.nonomldev.gestiontunicas.reparto.repository.RepartoRepository;
import es.nonomldev.gestiontunicas.reparto.repository.ReservaRepository;
import es.nonomldev.gestiontunicas.reparto.service.RepartoService;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunica;
import es.nonomldev.gestiontunicas.tunica.entity.Tunica;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RepartoServiceImpl implements RepartoService {

	private static final int JORNADAS_ORDINARIAS = 3;

	private final RepartoRepository repartoRepository;
	private final JornadaRepository jornadaRepository;
	private final ReservaRepository reservaRepository;
	private final CambioPeriodoDevolucionRepository cambioRepository;
	private final PlantillaResguardoRepository plantillaRepository;
	private final AlquilerRepository alquilerRepository;
	private final RequisitosAlquiler requisitos;
	private final RepartoMapper mapper;
	private final UsuarioActualService usuarioActualService;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public List<RepartoDto> listar() {
		return mapper.toDtos(repartoRepository.findAllByOrderByAnioDesc());
	}

	@Override
	@Transactional(readOnly = true)
	public RepartoDto obtener(Long id) {
		return mapper.toDto(buscar(id));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<RepartoDto> obtenerAbierto() {
		return repartoRepository.findFirstByEstado(EstadoReparto.ABIERTO).map(mapper::toDto);
	}

	@Override
	public RepartoDto crear(RepartoRequest request) {
		if (repartoRepository.existsByAnio(request.anio())) {
			throw new ReglaNegocioException("Ya existe el reparto de " + request.anio());
		}
		Reparto reparto = mapper.toEntity(request);
		reparto.setEstado(EstadoReparto.BORRADOR);
		fijarJuevesSanto(reparto, request);
		for (int i = 0; i < JORNADAS_ORDINARIAS; i++) {
			reparto.addJornada(jornadaOrdinaria(request.jornadas().get(i), i + 1));
		}
		return mapper.toDto(repartoRepository.save(reparto));
	}

	@Override
	public RepartoDto actualizar(Long id, RepartoRequest request) {
		Reparto reparto = buscar(id);
		exigirEstado(reparto, EstadoReparto.BORRADOR, "solo se puede modificar en borrador");
		if (!reparto.getAnio().equals(request.anio()) && repartoRepository.existsByAnio(request.anio())) {
			throw new ReglaNegocioException("Ya existe el reparto de " + request.anio());
		}
		mapper.actualizar(request, reparto);
		fijarJuevesSanto(reparto, request);
		// Las jornadas ordinarias se actualizan en su sitio (el número es único dentro del reparto)
		for (int i = 0; i < JORNADAS_ORDINARIAS; i++) {
			int numero = i + 1;
			JornadaRequest datos = request.jornadas().get(i);
			reparto.getJornadas().stream()
					.filter(j -> !j.isExtraordinaria() && j.getNumero() == numero)
					.findFirst()
					.ifPresentOrElse(j -> copiarJornada(datos, j), () -> reparto.addJornada(jornadaOrdinaria(datos, numero)));
		}
		return mapper.toDto(reparto);
	}

	@Override
	public RepartoDto asignarPlantilla(Long id, Long plantillaId) {
		Reparto reparto = buscar(id);
		if (reparto.getEstado() == EstadoReparto.CERRADO) {
			throw new ReglaNegocioException("El reparto de %d está cerrado".formatted(reparto.getAnio()));
		}
		reparto.setPlantillaResguardo(plantillaRepository.findById(plantillaId)
				.orElseThrow(() -> new RecursoNoEncontradoException("la plantilla del resguardo", plantillaId)));
		return mapper.toDto(reparto);
	}

	@Override
	public RepartoDto abrir(Long id) {
		Reparto reparto = buscar(id);
		exigirEstado(reparto, EstadoReparto.BORRADOR, "ya está abierto o cerrado");
		repartoRepository.findFirstByEstado(EstadoReparto.ABIERTO).ifPresent(otro -> {
			throw new ReglaNegocioException("El reparto de %d sigue abierto: ciérralo antes".formatted(otro.getAnio()));
		});
		if (reparto.getPlantillaResguardo() == null) {
			throw new ReglaNegocioException("Sube la plantilla del resguardo antes de abrir el reparto");
		}
		if (!reparto.isModoTransicion()) {
			reservarRenovaciones(reparto);
		}
		reparto.setEstado(EstadoReparto.ABIERTO);
		reparto.setFechaApertura(Instant.now(clock));
		return mapper.toDto(reparto);
	}

	@Override
	public RepartoDto liberarReservas(Long id) {
		Reparto reparto = buscar(id);
		exigirEstado(reparto, EstadoReparto.ABIERTO, "no está abierto");
		liberarReservasActivas(reparto);
		reparto.setReservasLiberadasEn(Instant.now(clock));
		return mapper.toDto(reparto);
	}

	@Override
	public RepartoDto cerrar(Long id) {
		Reparto reparto = buscar(id);
		exigirEstado(reparto, EstadoReparto.ABIERTO, "no está abierto");
		liberarReservasActivas(reparto);
		reparto.setEstado(EstadoReparto.CERRADO);
		reparto.setFechaCierre(Instant.now(clock));
		return mapper.toDto(reparto);
	}

	@Override
	public RepartoDto cambiarPeriodoDevolucion(Long id, CambioPeriodoDevolucionRequest request) {
		Reparto reparto = buscar(id);
		if (reparto.getFinGraciaProcesadoEn() != null) {
			throw new ReglaNegocioException("El plazo de gracia ya se ha cerrado: no se puede cambiar el periodo");
		}
		PeriodoDevolucion actual = reparto.getPeriodoDevolucion();
		PeriodoDevolucion nuevo = mapper.toEntity(request.periodo());

		CambioPeriodoDevolucion cambio = new CambioPeriodoDevolucion();
		cambio.setReparto(reparto);
		cambio.setAnterior(new PeriodoDevolucion(actual.getInicio(), actual.getFin(), actual.getPlazoGraciaMeses()));
		cambio.setNuevo(new PeriodoDevolucion(nuevo.getInicio(), nuevo.getFin(), nuevo.getPlazoGraciaMeses()));
		cambio.setMotivo(request.motivo());
		cambio.setUsuario(usuarioActualService.obtener());
		cambio.setFecha(Instant.now(clock));
		cambioRepository.save(cambio);

		reparto.setPeriodoDevolucion(nuevo);
		return mapper.toDto(reparto);
	}

	@Override
	public RepartoDto anadirJornadaExtraordinaria(Long id, JornadaRequest request) {
		Reparto reparto = buscar(id);
		if (reparto.getEstado() == EstadoReparto.CERRADO) {
			throw new ReglaNegocioException("El reparto de %d está cerrado".formatted(reparto.getAnio()));
		}
		if (reparto.jornadaDe(request.fecha()).isPresent()) {
			throw new ReglaNegocioException("Ya hay una jornada el " + request.fecha());
		}
		Jornada jornada = mapper.toEntity(request);
		jornada.setNumero(jornadaRepository.maxNumero(id) + 1);
		jornada.setExtraordinaria(true);
		reparto.addJornada(jornada);
		return mapper.toDto(reparto);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CambioPeriodoDevolucionDto> historialPeriodoDevolucion(Long id) {
		return mapper.toCambioDtos(cambioRepository.findByRepartoIdOrderByFechaDesc(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ReservaDto> reservas(Long id) {
		return mapper.toReservaDtos(reservaRepository.findByRepartoIdAndEstado(id, EstadoReserva.ACTIVA));
	}

	/**
	 * Reserva la túnica del año anterior a cada hermano que renueva. Si su túnica no está disponible
	 * (en reparación, incompleta...), no se reserva: la ficha lo avisa y el prioste le asigna otra.
	 */
	private void reservarRenovaciones(Reparto reparto) {
		Optional<Reparto> anterior = repartoRepository.findByAnio(reparto.getAnio() - 1);
		if (anterior.isEmpty()) {
			return;
		}
		Set<Long> hermanosReservados = new HashSet<>();
		for (Alquiler alquiler : alquilerRepository.findByRepartoIdAndEstado(anterior.get().getId(), EstadoAlquiler.DEVUELTO)) {
			Hermano hermano = alquiler.getHermano();
			Tunica tunica = alquiler.getTunica();
			boolean renueva = requisitos.alquilerQueDaRenovacion(hermano.getId(), reparto)
					.map(a -> a.getId().equals(alquiler.getId()))
					.orElse(false);
			if (!renueva || hermanosReservados.contains(hermano.getId())
					|| requisitos.motivoNoPuedeAlquilar(hermano, reparto).isPresent()
					|| tunica.getEstado() != EstadoTunica.DISPONIBLE) {
				continue;
			}
			Reserva reserva = new Reserva();
			reserva.setReparto(reparto);
			reserva.setHermano(hermano);
			reserva.setTunica(tunica);
			reserva.setAlquilerAnterior(alquiler);
			reserva.setEstado(EstadoReserva.ACTIVA);
			reservaRepository.save(reserva);
			tunica.setEstado(EstadoTunica.RESERVADA);
			hermanosReservados.add(hermano.getId());
		}
	}

	private void liberarReservasActivas(Reparto reparto) {
		Instant ahora = Instant.now(clock);
		for (Reserva reserva : reservaRepository.findByRepartoIdAndEstado(reparto.getId(), EstadoReserva.ACTIVA)) {
			reserva.setEstado(EstadoReserva.LIBERADA);
			reserva.setResueltaEn(ahora);
			if (reserva.getTunica().getEstado() == EstadoTunica.RESERVADA) {
				reserva.getTunica().setEstado(EstadoTunica.DISPONIBLE);
			}
		}
	}

	private static void fijarJuevesSanto(Reparto reparto, RepartoRequest request) {
		boolean manual = request.fechaJuevesSanto() != null;
		reparto.setJuevesSantoManual(manual);
		reparto.setFechaJuevesSanto(manual ? request.fechaJuevesSanto() : Reparto.calcularJuevesSanto(request.anio()));
	}

	private Jornada jornadaOrdinaria(JornadaRequest datos, int numero) {
		Jornada jornada = mapper.toEntity(datos);
		jornada.setNumero(numero);
		jornada.setExtraordinaria(false);
		jornada.setLimosna(null); // las ordinarias usan la limosna del reparto
		return jornada;
	}

	private static void copiarJornada(JornadaRequest datos, Jornada jornada) {
		jornada.setFecha(datos.fecha());
		jornada.setHoraInicio(datos.horaInicio());
		jornada.setHoraFin(datos.horaFin());
		jornada.setPermiteRenovacion(datos.permiteRenovacion());
		jornada.setPermiteNuevaAlta(datos.permiteNuevaAlta());
		jornada.setPermiteCambio(datos.permiteCambio());
	}

	private Reparto buscar(Long id) {
		return repartoRepository.findWithJornadasById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("el reparto", id));
	}

	private static void exigirEstado(Reparto reparto, EstadoReparto estado, String mensaje) {
		if (reparto.getEstado() != estado) {
			throw new ReglaNegocioException("El reparto de %d %s".formatted(reparto.getAnio(), mensaje));
		}
	}

}
