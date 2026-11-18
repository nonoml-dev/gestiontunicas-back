package es.nonomldev.gestiontunicas.sancion.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import es.nonomldev.gestiontunicas.alquiler.entity.Alquiler;
import es.nonomldev.gestiontunicas.devolucion.entity.Devolucion;
import es.nonomldev.gestiontunicas.reparto.entity.Reparto;
import es.nonomldev.gestiontunicas.sancion.entity.ConceptoSancion;
import es.nonomldev.gestiontunicas.sancion.entity.EstadoSancion;
import es.nonomldev.gestiontunicas.sancion.entity.OrigenVeto;
import es.nonomldev.gestiontunicas.sancion.entity.Sancion;
import es.nonomldev.gestiontunicas.sancion.entity.Veto;
import es.nonomldev.gestiontunicas.sancion.repository.SancionRepository;
import es.nonomldev.gestiontunicas.sancion.repository.VetoRepository;
import es.nonomldev.gestiontunicas.seguridad.entity.Usuario;
import lombok.RequiredArgsConstructor;

/**
 * Crea sanciones y vetos automáticos sin duplicarlos: cada concepto se cobra una vez por alquiler
 * y cada alquiler no devuelto en plazo genera un solo veto.
 */
@Component
@RequiredArgsConstructor
public class GeneradorSanciones {

	private final SancionRepository sancionRepository;
	private final VetoRepository vetoRepository;
	private final Clock clock;

	/** Crea la sanción con el importe del reparto, salvo que ya exista ese concepto para el alquiler. */
	public Optional<Sancion> sancionar(Alquiler alquiler, Devolucion devolucion, ConceptoSancion concepto) {
		if (sancionRepository.existsByAlquilerIdAndConceptoAndEstadoNot(alquiler.getId(), concepto, EstadoSancion.ANULADA)) {
			return Optional.empty();
		}
		Reparto reparto = alquiler.getReparto();
		BigDecimal importe = reparto.getTarifas().importe(concepto);
		Sancion sancion = new Sancion();
		sancion.setHermano(alquiler.getHermano());
		sancion.setReparto(reparto);
		sancion.setAlquiler(alquiler);
		sancion.setDevolucion(devolucion);
		sancion.setConcepto(concepto);
		sancion.setImporteCalculado(importe);
		sancion.setImporte(importe);
		sancion.setEstado(EstadoSancion.PENDIENTE);
		return Optional.of(sancionRepository.save(sancion));
	}

	/** Veto automático por no devolver en plazo: solo para el reparto del año siguiente. */
	public Optional<Veto> vetarPorNoDevolverEnPlazo(Alquiler alquiler, Usuario usuario) {
		if (vetoRepository.existsByAlquilerId(alquiler.getId())) {
			return Optional.empty();
		}
		Reparto reparto = alquiler.getReparto();
		Veto veto = new Veto();
		veto.setHermano(alquiler.getHermano());
		veto.setOrigen(OrigenVeto.NO_DEVOLUCION_EN_PLAZO);
		veto.setAnioVetado(reparto.getAnio() + 1);
		veto.setAlquiler(alquiler);
		veto.setMotivo("No devolvió la túnica %s en plazo en el reparto %d"
				.formatted(alquiler.getTunica().getCodigo(), reparto.getAnio()));
		veto.setFecha(Instant.now(clock));
		veto.setUsuario(usuario);
		veto.setActivo(true);
		return Optional.of(vetoRepository.save(veto));
	}

}
