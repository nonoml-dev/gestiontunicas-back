package es.nonomldev.gestiontunicas.devolucion.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import es.nonomldev.gestiontunicas.alquiler.entity.Alquiler;
import es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquiler;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.devolucion.dto.RevisionPiezaRequest;
import es.nonomldev.gestiontunicas.devolucion.entity.Devolucion;
import es.nonomldev.gestiontunicas.devolucion.entity.ResultadoRevision;
import es.nonomldev.gestiontunicas.devolucion.entity.RevisionPieza;
import es.nonomldev.gestiontunicas.devolucion.mapper.DevolucionMapper;
import es.nonomldev.gestiontunicas.devolucion.repository.DevolucionRepository;
import es.nonomldev.gestiontunicas.reparto.entity.PeriodoDevolucion;
import es.nonomldev.gestiontunicas.sancion.service.CalculadoraSanciones;
import es.nonomldev.gestiontunicas.sancion.service.GeneradorSanciones;
import es.nonomldev.gestiontunicas.seguridad.entity.Usuario;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoPieza;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunica;
import es.nonomldev.gestiontunicas.tunica.entity.Pieza;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPieza;
import es.nonomldev.gestiontunicas.tunica.entity.Tunica;
import lombok.RequiredArgsConstructor;

/**
 * Registra una devolución con la revisión pieza a pieza. Lo usan la devolución normal y el cambio
 * de túnica (devolución rápida, que no puede ir fuera de plazo).
 */
@Component
@RequiredArgsConstructor
public class ProcesadorDevolucion {

	private static final Set<EstadoAlquiler> DEVOLVIBLES = EnumSet.of(EstadoAlquiler.ACTIVO, EstadoAlquiler.NO_DEVUELTO);

	private final DevolucionRepository devolucionRepository;
	private final DevolucionMapper mapper;
	private final CalculadoraSanciones calculadora;
	private final GeneradorSanciones generador;
	private final Clock clock;

	public Devolucion procesar(Alquiler alquiler, List<RevisionPiezaRequest> revisiones, String observaciones,
			boolean porCambio, Usuario usuario) {
		if (!DEVOLVIBLES.contains(alquiler.getEstado())) {
			throw new ReglaNegocioException(switch (alquiler.getEstado()) {
				case ADQUIRIDA_POR_HERMANO -> "La túnica %s ya es propiedad del hermano: no se registra como devolución"
						.formatted(alquiler.getTunica().getCodigo());
				default -> "La túnica %s ya se devolvió".formatted(alquiler.getTunica().getCodigo());
			});
		}
		if (devolucionRepository.existsByAlquilerId(alquiler.getId())) {
			throw new ReglaNegocioException("Esta entrega ya tiene una devolución registrada");
		}
		Tunica tunica = alquiler.getTunica();
		Map<TipoPieza, Pieza> piezas = tunica.getPiezas().stream()
				.collect(Collectors.toMap(Pieza::getTipo, Function.identity()));
		Set<TipoPieza> revisadas = revisiones.stream().map(RevisionPiezaRequest::tipoPieza).collect(Collectors.toSet());
		if (revisadas.size() != TipoPieza.values().length || !piezas.keySet().equals(revisadas)) {
			throw new ReglaNegocioException("Hay que revisar las 5 piezas: túnica, capa, antifaz, cíngulo y talega");
		}

		LocalDate hoy = LocalDate.now(clock);
		PeriodoDevolucion periodo = alquiler.getReparto().getPeriodoDevolucion();
		boolean fueraPlazo = !porCambio && periodo.fueraDePlazo(hoy);
		CalculadoraSanciones.Resultado calculo = calculadora.calcular(revisiones, fueraPlazo);

		Devolucion devolucion = new Devolucion();
		devolucion.setAlquiler(alquiler);
		devolucion.setFecha(Instant.now(clock));
		devolucion.setUsuario(usuario);
		devolucion.setPorCambio(porCambio);
		devolucion.setFueraPlazo(fueraPlazo);
		devolucion.setDiasRetraso(fueraPlazo ? periodo.diasRetraso(hoy) : 0);
		devolucion.setGravedad(calculo.gravedad());
		devolucion.setObservaciones(observaciones);
		for (RevisionPiezaRequest datos : revisiones) {
			RevisionPieza revision = mapper.toEntity(datos);
			Pieza pieza = piezas.get(datos.tipoPieza());
			revision.setPieza(pieza);
			devolucion.addRevision(revision);
			pieza.setEstado(estadoPieza(datos));
		}
		devolucionRepository.save(devolucion);

		tunica.setEstado(estadoTunica(tunica));
		alquiler.setEstado(EstadoAlquiler.DEVUELTO);
		calculo.conceptos().forEach(concepto -> generador.sancionar(alquiler, devolucion, concepto));
		if (fueraPlazo) {
			generador.vetarPorNoDevolverEnPlazo(alquiler, usuario);
		}
		return devolucion;
	}

	private static EstadoPieza estadoPieza(RevisionPiezaRequest revision) {
		if (revision.resultado() == ResultadoRevision.NO_ENTREGADA) {
			return EstadoPieza.PERDIDA;
		}
		if (revision.resultado() == ResultadoRevision.DETERIORADA || revision.defectoCapa() != null) {
			return EstadoPieza.DETERIORADA;
		}
		return EstadoPieza.BUEN_ESTADO;
	}

	/** Pieza perdida → incompleta; pieza deteriorada → en reparación; todo bien → disponible. */
	private static EstadoTunica estadoTunica(Tunica tunica) {
		if (tunica.getPiezas().stream().anyMatch(p -> p.getEstado() == EstadoPieza.PERDIDA)) {
			return EstadoTunica.INCOMPLETA;
		}
		if (tunica.getPiezas().stream().anyMatch(p -> p.getEstado() != EstadoPieza.BUEN_ESTADO)) {
			return EstadoTunica.EN_REPARACION;
		}
		return EstadoTunica.DISPONIBLE;
	}

}
