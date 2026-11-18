package es.nonomldev.gestiontunicas.sancion.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Component;

import es.nonomldev.gestiontunicas.alquiler.entity.AlquilerEntity;
import es.nonomldev.gestiontunicas.devolucion.entity.DevolucionEntity;
import es.nonomldev.gestiontunicas.reparto.entity.RepartoEntity;
import es.nonomldev.gestiontunicas.sancion.entity.ConceptoSancionEnum;
import es.nonomldev.gestiontunicas.sancion.entity.EstadoSancionEnum;
import es.nonomldev.gestiontunicas.sancion.entity.OrigenVetoEnum;
import es.nonomldev.gestiontunicas.sancion.entity.SancionEntity;
import es.nonomldev.gestiontunicas.sancion.entity.VetoEntity;
import es.nonomldev.gestiontunicas.sancion.repository.SancionRepository;
import es.nonomldev.gestiontunicas.sancion.repository.VetoRepository;
import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;
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
	public Optional<SancionEntity> sancionar(AlquilerEntity alquiler, DevolucionEntity devolucion, ConceptoSancionEnum concepto) {
		if (sancionRepository.existsByAlquilerIdAndConceptoAndEstadoNot(alquiler.getId(), concepto, EstadoSancionEnum.ANULADA)) {
			return Optional.empty();
		}
		RepartoEntity reparto = alquiler.getReparto();
		BigDecimal importe = reparto.getTarifas().importe(concepto);
		SancionEntity sancion = new SancionEntity();
		sancion.setHermano(alquiler.getHermano());
		sancion.setReparto(reparto);
		sancion.setAlquiler(alquiler);
		sancion.setDevolucion(devolucion);
		sancion.setConcepto(concepto);
		sancion.setImporteCalculado(importe);
		sancion.setImporte(importe);
		sancion.setEstado(EstadoSancionEnum.PENDIENTE);
		return Optional.of(sancionRepository.save(sancion));
	}

	/** Veto automático por no devolver en plazo: solo para el reparto del año siguiente. */
	public Optional<VetoEntity> vetarPorNoDevolverEnPlazo(AlquilerEntity alquiler, UsuarioEntity usuario) {
		if (vetoRepository.existsByAlquilerId(alquiler.getId())) {
			return Optional.empty();
		}
		RepartoEntity reparto = alquiler.getReparto();
		VetoEntity veto = new VetoEntity();
		veto.setHermano(alquiler.getHermano());
		veto.setOrigen(OrigenVetoEnum.NO_DEVOLUCION_EN_PLAZO);
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
