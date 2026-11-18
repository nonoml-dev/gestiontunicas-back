package es.nonomldev.gestiontunicas.reparto.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.alquiler.entity.AlquilerEntity;
import es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquilerEnum;
import es.nonomldev.gestiontunicas.alquiler.repository.AlquilerRepository;
import es.nonomldev.gestiontunicas.alquiler.service.RequisitosAlquiler;
import es.nonomldev.gestiontunicas.comun.exception.RecursoNoEncontradoException;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.hermano.entity.HermanoEntity;
import es.nonomldev.gestiontunicas.reparto.dto.CambioPeriodoDevolucionDTO;
import es.nonomldev.gestiontunicas.reparto.dto.CambioPeriodoDevolucionRequest;
import es.nonomldev.gestiontunicas.reparto.dto.JornadaRequest;
import es.nonomldev.gestiontunicas.reparto.dto.RepartoDTO;
import es.nonomldev.gestiontunicas.reparto.dto.RepartoRequest;
import es.nonomldev.gestiontunicas.reparto.dto.ReservaDTO;
import es.nonomldev.gestiontunicas.reparto.entity.CambioPeriodoDevolucionEntity;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoRepartoEnum;
import es.nonomldev.gestiontunicas.reparto.entity.EstadoReservaEnum;
import es.nonomldev.gestiontunicas.reparto.entity.JornadaEntity;
import es.nonomldev.gestiontunicas.reparto.entity.PeriodoDevolucionEntity;
import es.nonomldev.gestiontunicas.reparto.entity.RepartoEntity;
import es.nonomldev.gestiontunicas.reparto.entity.ReservaEntity;
import es.nonomldev.gestiontunicas.reparto.mapper.RepartoMapper;
import es.nonomldev.gestiontunicas.reparto.repository.CambioPeriodoDevolucionRepository;
import es.nonomldev.gestiontunicas.reparto.repository.JornadaRepository;
import es.nonomldev.gestiontunicas.reparto.repository.PlantillaResguardoRepository;
import es.nonomldev.gestiontunicas.reparto.repository.RepartoRepository;
import es.nonomldev.gestiontunicas.reparto.repository.ReservaRepository;
import es.nonomldev.gestiontunicas.reparto.service.RepartoService;
import es.nonomldev.gestiontunicas.seguridad.service.UsuarioActualService;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunicaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TunicaEntity;
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
	public List<RepartoDTO> listar() {
		return mapper.toDTOs(repartoRepository.findAllByOrderByAnioDesc());
	}

	@Override
	@Transactional(readOnly = true)
	public RepartoDTO obtener(Long id) {
		return mapper.toDTO(buscar(id));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<RepartoDTO> obtenerAbierto() {
		return repartoRepository.findFirstByEstado(EstadoRepartoEnum.ABIERTO).map(mapper::toDTO);
	}

	@Override
	public RepartoDTO crear(RepartoRequest request) {
		if (repartoRepository.existsByAnio(request.anio())) {
			throw new ReglaNegocioException("Ya existe el reparto de " + request.anio());
		}
		RepartoEntity reparto = mapper.toEntity(request);
		reparto.setEstado(EstadoRepartoEnum.BORRADOR);
		fijarJuevesSanto(reparto, request);
		for (int i = 0; i < JORNADAS_ORDINARIAS; i++) {
			reparto.addJornada(jornadaOrdinaria(request.jornadas().get(i), i + 1));
		}
		return mapper.toDTO(repartoRepository.save(reparto));
	}

	@Override
	public RepartoDTO actualizar(Long id, RepartoRequest request) {
		RepartoEntity reparto = buscar(id);
		exigirEstado(reparto, EstadoRepartoEnum.BORRADOR, "solo se puede modificar en borrador");
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
		return mapper.toDTO(reparto);
	}

	@Override
	public RepartoDTO asignarPlantilla(Long id, Long plantillaId) {
		RepartoEntity reparto = buscar(id);
		if (reparto.getEstado() == EstadoRepartoEnum.CERRADO) {
			throw new ReglaNegocioException("El reparto de %d está cerrado".formatted(reparto.getAnio()));
		}
		reparto.setPlantillaResguardo(plantillaRepository.findById(plantillaId)
				.orElseThrow(() -> new RecursoNoEncontradoException("la plantilla del resguardo", plantillaId)));
		return mapper.toDTO(reparto);
	}

	@Override
	public RepartoDTO abrir(Long id) {
		RepartoEntity reparto = buscar(id);
		exigirEstado(reparto, EstadoRepartoEnum.BORRADOR, "ya está abierto o cerrado");
		repartoRepository.findFirstByEstado(EstadoRepartoEnum.ABIERTO).ifPresent(otro -> {
			throw new ReglaNegocioException("El reparto de %d sigue abierto: ciérralo antes".formatted(otro.getAnio()));
		});
		if (reparto.getPlantillaResguardo() == null) {
			throw new ReglaNegocioException("Sube la plantilla del resguardo antes de abrir el reparto");
		}
		if (!reparto.isModoTransicion()) {
			reservarRenovaciones(reparto);
		}
		reparto.setEstado(EstadoRepartoEnum.ABIERTO);
		reparto.setFechaApertura(Instant.now(clock));
		return mapper.toDTO(reparto);
	}

	@Override
	public RepartoDTO liberarReservas(Long id) {
		RepartoEntity reparto = buscar(id);
		exigirEstado(reparto, EstadoRepartoEnum.ABIERTO, "no está abierto");
		liberarReservasActivas(reparto);
		reparto.setReservasLiberadasEn(Instant.now(clock));
		return mapper.toDTO(reparto);
	}

	@Override
	public RepartoDTO cerrar(Long id) {
		RepartoEntity reparto = buscar(id);
		exigirEstado(reparto, EstadoRepartoEnum.ABIERTO, "no está abierto");
		liberarReservasActivas(reparto);
		reparto.setEstado(EstadoRepartoEnum.CERRADO);
		reparto.setFechaCierre(Instant.now(clock));
		return mapper.toDTO(reparto);
	}

	@Override
	public RepartoDTO cambiarPeriodoDevolucion(Long id, CambioPeriodoDevolucionRequest request) {
		RepartoEntity reparto = buscar(id);
		if (reparto.getFinGraciaProcesadoEn() != null) {
			throw new ReglaNegocioException("El plazo de gracia ya se ha cerrado: no se puede cambiar el periodo");
		}
		PeriodoDevolucionEntity actual = reparto.getPeriodoDevolucion();
		PeriodoDevolucionEntity nuevo = mapper.toEntity(request.periodo());

		CambioPeriodoDevolucionEntity cambio = new CambioPeriodoDevolucionEntity();
		cambio.setReparto(reparto);
		cambio.setAnterior(new PeriodoDevolucionEntity(actual.getInicio(), actual.getFin(), actual.getPlazoGraciaMeses()));
		cambio.setNuevo(new PeriodoDevolucionEntity(nuevo.getInicio(), nuevo.getFin(), nuevo.getPlazoGraciaMeses()));
		cambio.setMotivo(request.motivo());
		cambio.setUsuario(usuarioActualService.obtener());
		cambio.setFecha(Instant.now(clock));
		cambioRepository.save(cambio);

		reparto.setPeriodoDevolucion(nuevo);
		return mapper.toDTO(reparto);
	}

	@Override
	public RepartoDTO anadirJornadaExtraordinaria(Long id, JornadaRequest request) {
		RepartoEntity reparto = buscar(id);
		if (reparto.getEstado() == EstadoRepartoEnum.CERRADO) {
			throw new ReglaNegocioException("El reparto de %d está cerrado".formatted(reparto.getAnio()));
		}
		if (reparto.jornadaDe(request.fecha()).isPresent()) {
			throw new ReglaNegocioException("Ya hay una jornada el " + request.fecha());
		}
		JornadaEntity jornada = mapper.toEntity(request);
		jornada.setNumero(jornadaRepository.maxNumero(id) + 1);
		jornada.setExtraordinaria(true);
		reparto.addJornada(jornada);
		return mapper.toDTO(reparto);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CambioPeriodoDevolucionDTO> historialPeriodoDevolucion(Long id) {
		return mapper.toCambioDTOs(cambioRepository.findByRepartoIdOrderByFechaDesc(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ReservaDTO> reservas(Long id) {
		return mapper.toReservaDTOs(reservaRepository.findByRepartoIdAndEstado(id, EstadoReservaEnum.ACTIVA));
	}

	/**
	 * Reserva la túnica del año anterior a cada hermano que renueva. Si su túnica no está disponible
	 * (en reparación, incompleta...), no se reserva: la ficha lo avisa y el prioste le asigna otra.
	 */
	private void reservarRenovaciones(RepartoEntity reparto) {
		Optional<RepartoEntity> anterior = repartoRepository.findByAnio(reparto.getAnio() - 1);
		if (anterior.isEmpty()) {
			return;
		}
		Set<Long> hermanosReservados = new HashSet<>();
		for (AlquilerEntity alquiler : alquilerRepository.findByRepartoIdAndEstado(anterior.get().getId(), EstadoAlquilerEnum.DEVUELTO)) {
			HermanoEntity hermano = alquiler.getHermano();
			TunicaEntity tunica = alquiler.getTunica();
			boolean renueva = requisitos.alquilerQueDaRenovacion(hermano.getId(), reparto)
					.map(a -> a.getId().equals(alquiler.getId()))
					.orElse(false);
			if (!renueva || hermanosReservados.contains(hermano.getId())
					|| requisitos.motivoNoPuedeAlquilar(hermano, reparto).isPresent()
					|| tunica.getEstado() != EstadoTunicaEnum.DISPONIBLE) {
				continue;
			}
			ReservaEntity reserva = new ReservaEntity();
			reserva.setReparto(reparto);
			reserva.setHermano(hermano);
			reserva.setTunica(tunica);
			reserva.setAlquilerAnterior(alquiler);
			reserva.setEstado(EstadoReservaEnum.ACTIVA);
			reservaRepository.save(reserva);
			tunica.setEstado(EstadoTunicaEnum.RESERVADA);
			hermanosReservados.add(hermano.getId());
		}
	}

	private void liberarReservasActivas(RepartoEntity reparto) {
		Instant ahora = Instant.now(clock);
		for (ReservaEntity reserva : reservaRepository.findByRepartoIdAndEstado(reparto.getId(), EstadoReservaEnum.ACTIVA)) {
			reserva.setEstado(EstadoReservaEnum.LIBERADA);
			reserva.setResueltaEn(ahora);
			if (reserva.getTunica().getEstado() == EstadoTunicaEnum.RESERVADA) {
				reserva.getTunica().setEstado(EstadoTunicaEnum.DISPONIBLE);
			}
		}
	}

	private static void fijarJuevesSanto(RepartoEntity reparto, RepartoRequest request) {
		boolean manual = request.fechaJuevesSanto() != null;
		reparto.setJuevesSantoManual(manual);
		reparto.setFechaJuevesSanto(manual ? request.fechaJuevesSanto() : RepartoEntity.calcularJuevesSanto(request.anio()));
	}

	private JornadaEntity jornadaOrdinaria(JornadaRequest datos, int numero) {
		JornadaEntity jornada = mapper.toEntity(datos);
		jornada.setNumero(numero);
		jornada.setExtraordinaria(false);
		jornada.setLimosna(null); // las ordinarias usan la limosna del reparto
		return jornada;
	}

	private static void copiarJornada(JornadaRequest datos, JornadaEntity jornada) {
		jornada.setFecha(datos.fecha());
		jornada.setHoraInicio(datos.horaInicio());
		jornada.setHoraFin(datos.horaFin());
		jornada.setPermiteRenovacion(datos.permiteRenovacion());
		jornada.setPermiteNuevaAlta(datos.permiteNuevaAlta());
		jornada.setPermiteCambio(datos.permiteCambio());
	}

	private RepartoEntity buscar(Long id) {
		return repartoRepository.findWithJornadasById(id)
				.orElseThrow(() -> new RecursoNoEncontradoException("el reparto", id));
	}

	private static void exigirEstado(RepartoEntity reparto, EstadoRepartoEnum estado, String mensaje) {
		if (reparto.getEstado() != estado) {
			throw new ReglaNegocioException("El reparto de %d %s".formatted(reparto.getAnio(), mensaje));
		}
	}

}
