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

import es.nonomldev.gestiontunicas.alquiler.entity.AlquilerEntity;
import es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquilerEnum;
import es.nonomldev.gestiontunicas.comun.exception.ReglaNegocioException;
import es.nonomldev.gestiontunicas.devolucion.dto.RevisionPiezaRequest;
import es.nonomldev.gestiontunicas.devolucion.entity.DevolucionEntity;
import es.nonomldev.gestiontunicas.devolucion.entity.ResultadoRevisionEnum;
import es.nonomldev.gestiontunicas.devolucion.entity.RevisionPiezaEntity;
import es.nonomldev.gestiontunicas.devolucion.mapper.DevolucionMapper;
import es.nonomldev.gestiontunicas.devolucion.repository.DevolucionRepository;
import es.nonomldev.gestiontunicas.reparto.entity.PeriodoDevolucionEntity;
import es.nonomldev.gestiontunicas.sancion.service.CalculadoraSanciones;
import es.nonomldev.gestiontunicas.sancion.service.GeneradorSanciones;
import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoPiezaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.EstadoTunicaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.PiezaEntity;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPiezaEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TunicaEntity;
import lombok.RequiredArgsConstructor;

/**
 * Registra una devolución con la revisión pieza a pieza. Lo usan la devolución normal y el cambio
 * de túnica (devolución rápida, que no puede ir fuera de plazo).
 */
@Component
@RequiredArgsConstructor
public class ProcesadorDevolucion {

	private static final Set<EstadoAlquilerEnum> DEVOLVIBLES = EnumSet.of(EstadoAlquilerEnum.ACTIVO, EstadoAlquilerEnum.NO_DEVUELTO);

	private final DevolucionRepository devolucionRepository;
	private final DevolucionMapper mapper;
	private final CalculadoraSanciones calculadora;
	private final GeneradorSanciones generador;
	private final Clock clock;

	public DevolucionEntity procesar(AlquilerEntity alquiler, List<RevisionPiezaRequest> revisiones, String observaciones,
			boolean porCambio, UsuarioEntity usuario) {
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
		TunicaEntity tunica = alquiler.getTunica();
		Map<TipoPiezaEnum, PiezaEntity> piezas = tunica.getPiezas().stream()
				.collect(Collectors.toMap(PiezaEntity::getTipo, Function.identity()));
		Set<TipoPiezaEnum> revisadas = revisiones.stream().map(RevisionPiezaRequest::tipoPieza).collect(Collectors.toSet());
		if (revisadas.size() != TipoPiezaEnum.values().length || !piezas.keySet().equals(revisadas)) {
			throw new ReglaNegocioException("Hay que revisar las 5 piezas: túnica, capa, antifaz, cíngulo y talega");
		}

		LocalDate hoy = LocalDate.now(clock);
		PeriodoDevolucionEntity periodo = alquiler.getReparto().getPeriodoDevolucion();
		boolean fueraPlazo = !porCambio && periodo.fueraDePlazo(hoy);
		CalculadoraSanciones.Resultado calculo = calculadora.calcular(revisiones, fueraPlazo);

		DevolucionEntity devolucion = new DevolucionEntity();
		devolucion.setAlquiler(alquiler);
		devolucion.setFecha(Instant.now(clock));
		devolucion.setUsuario(usuario);
		devolucion.setPorCambio(porCambio);
		devolucion.setFueraPlazo(fueraPlazo);
		devolucion.setDiasRetraso(fueraPlazo ? periodo.diasRetraso(hoy) : 0);
		devolucion.setGravedad(calculo.gravedad());
		devolucion.setObservaciones(observaciones);
		for (RevisionPiezaRequest datos : revisiones) {
			RevisionPiezaEntity revision = mapper.toEntity(datos);
			PiezaEntity pieza = piezas.get(datos.tipoPieza());
			revision.setPieza(pieza);
			devolucion.addRevision(revision);
			pieza.setEstado(estadoPieza(datos));
		}
		devolucionRepository.save(devolucion);

		tunica.setEstado(estadoTunica(tunica));
		alquiler.setEstado(EstadoAlquilerEnum.DEVUELTO);
		calculo.conceptos().forEach(concepto -> generador.sancionar(alquiler, devolucion, concepto));
		if (fueraPlazo) {
			generador.vetarPorNoDevolverEnPlazo(alquiler, usuario);
		}
		return devolucion;
	}

	private static EstadoPiezaEnum estadoPieza(RevisionPiezaRequest revision) {
		if (revision.resultado() == ResultadoRevisionEnum.NO_ENTREGADA) {
			return EstadoPiezaEnum.PERDIDA;
		}
		if (revision.resultado() == ResultadoRevisionEnum.DETERIORADA || revision.defectoCapa() != null) {
			return EstadoPiezaEnum.DETERIORADA;
		}
		return EstadoPiezaEnum.BUEN_ESTADO;
	}

	/** Pieza perdida → incompleta; pieza deteriorada → en reparación; todo bien → disponible. */
	private static EstadoTunicaEnum estadoTunica(TunicaEntity tunica) {
		if (tunica.getPiezas().stream().anyMatch(p -> p.getEstado() == EstadoPiezaEnum.PERDIDA)) {
			return EstadoTunicaEnum.INCOMPLETA;
		}
		if (tunica.getPiezas().stream().anyMatch(p -> p.getEstado() != EstadoPiezaEnum.BUEN_ESTADO)) {
			return EstadoTunicaEnum.EN_REPARACION;
		}
		return EstadoTunicaEnum.DISPONIBLE;
	}

}
