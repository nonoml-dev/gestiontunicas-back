package es.nonomldev.gestiontunicas.alquiler.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import es.nonomldev.gestiontunicas.alquiler.entity.AlquilerEntity;
import es.nonomldev.gestiontunicas.alquiler.entity.EstadoAlquilerEnum;
import es.nonomldev.gestiontunicas.alquiler.repository.AlquilerRepository;
import es.nonomldev.gestiontunicas.comun.util.Formato;
import es.nonomldev.gestiontunicas.devolucion.repository.DevolucionRepository;
import es.nonomldev.gestiontunicas.hermano.entity.HermanoEntity;
import es.nonomldev.gestiontunicas.reparto.entity.RepartoEntity;
import es.nonomldev.gestiontunicas.sancion.entity.EstadoSancionEnum;
import es.nonomldev.gestiontunicas.sancion.repository.SancionRepository;
import es.nonomldev.gestiontunicas.sancion.repository.VetoRepository;
import lombok.RequiredArgsConstructor;

/**
 * Reglas para alquilar (análisis §4.1 y §4.2). Las usan la apertura del reparto (reservas de quien
 * renueva) y la ficha del hermano (qué le toca o el motivo exacto por el que no puede).
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequisitosAlquiler {

	private final VetoRepository vetoRepository;
	private final SancionRepository sancionRepository;
	private final AlquilerRepository alquilerRepository;
	private final DevolucionRepository devolucionRepository;

	/** Motivo exacto por el que el hermano no puede alquilar en el reparto, o vacío si puede. */
	public Optional<String> motivoNoPuedeAlquilar(HermanoEntity hermano, RepartoEntity reparto) {
		if (!hermano.isActivo()) {
			return Optional.of("El hermano está dado de baja");
		}
		if (!hermano.fichaCompleta()) {
			return Optional.of("Ficha incompleta: falta la fecha de nacimiento");
		}
		if (!reparto.edadPermitida(hermano)) {
			return Optional.of("Fuera de edad: tendrá %d años el Jueves Santo (%s)".formatted(
					hermano.edadA(reparto.getFechaJuevesSanto()), reparto.getFechaJuevesSanto().format(Formato.FECHA)));
		}
		Optional<String> veto = vetoRepository.findFirstByHermanoIdAndAnioVetadoAndActivoTrue(hermano.getId(), reparto.getAnio())
				.map(v -> "Vetado para el reparto %d: %s".formatted(reparto.getAnio(), v.getMotivo()));
		if (veto.isPresent()) {
			return veto;
		}
		if (sancionRepository.existsByHermanoIdAndEstado(hermano.getId(), EstadoSancionEnum.PENDIENTE)) {
			BigDecimal total = sancionRepository.totalPendiente(hermano.getId());
			return Optional.of("Sanción pendiente de " + Formato.euros(total));
		}
		return Optional.empty();
	}

	/**
	 * Alquiler del año anterior que le da derecho a RENOVACIÓN: lo devolvió en plazo, con o sin
	 * incidencias (las sanciones pendientes ya se comprueban aparte). Vacío = nueva alta.
	 */
	public Optional<AlquilerEntity> alquilerQueDaRenovacion(Long hermanoId, RepartoEntity reparto) {
		return alquilerRepository.findUltimoDelAnio(hermanoId, reparto.getAnio() - 1)
				.filter(a -> a.getEstado() == EstadoAlquilerEnum.DEVUELTO)
				.filter(a -> devolucionRepository.findByAlquilerId(a.getId())
						.map(d -> !d.isFueraPlazo())
						.orElse(false));
	}

}
