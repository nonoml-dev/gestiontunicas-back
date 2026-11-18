package es.nonomldev.gestiontunicas.sancion.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import es.nonomldev.gestiontunicas.devolucion.dto.RevisionPiezaRequest;
import es.nonomldev.gestiontunicas.devolucion.entity.Gravedad;
import es.nonomldev.gestiontunicas.devolucion.entity.ResultadoRevision;
import es.nonomldev.gestiontunicas.sancion.entity.ConceptoSancion;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPieza;

/**
 * Reglas de sanción de la devolución (análisis §4.8):
 * <ul>
 * <li>A: túnica, capa (incluido el escudo JHS) o antifaz deteriorada o no entregada → una sola vez.</li>
 * <li>B: talega no entregada.</li>
 * <li>D: cíngulo no entregado o deteriorado.</li>
 * <li>E: devuelta sucia.</li>
 * <li>C: fuera de plazo.</li>
 * </ul>
 * Gravedad: fuera de plazo = muy grave; A, B o D = grave; solo E = leve.
 */
@Component
public class CalculadoraSanciones {

	public record Resultado(List<ConceptoSancion> conceptos, Gravedad gravedad) {
	}

	public Resultado calcular(List<RevisionPiezaRequest> revisiones, boolean fueraPlazo) {
		List<ConceptoSancion> conceptos = new ArrayList<>();

		boolean principalDanada = revisiones.stream()
				.filter(r -> r.tipoPieza().esPrincipal())
				.anyMatch(r -> danada(r) || r.resultado() == ResultadoRevision.NO_ENTREGADA);
		if (principalDanada) {
			conceptos.add(ConceptoSancion.PIEZA_PRINCIPAL);
		}
		if (revisiones.stream().anyMatch(r -> r.tipoPieza() == TipoPieza.TALEGA
				&& r.resultado() == ResultadoRevision.NO_ENTREGADA)) {
			conceptos.add(ConceptoSancion.TALEGA);
		}
		if (revisiones.stream().anyMatch(r -> r.tipoPieza() == TipoPieza.CINGULO
				&& (danada(r) || r.resultado() == ResultadoRevision.NO_ENTREGADA))) {
			conceptos.add(ConceptoSancion.CINGULO);
		}
		boolean sucia = revisiones.stream().anyMatch(r -> r.resultado() == ResultadoRevision.SUCIA);
		if (sucia) {
			conceptos.add(ConceptoSancion.SUCIEDAD);
		}
		if (fueraPlazo) {
			conceptos.add(ConceptoSancion.RETRASO);
		}

		Gravedad gravedad;
		if (fueraPlazo) {
			gravedad = Gravedad.MUY_GRAVE;
		} else if (conceptos.stream().anyMatch(c -> c != ConceptoSancion.SUCIEDAD)) {
			gravedad = Gravedad.GRAVE;
		} else if (sucia) {
			gravedad = Gravedad.LEVE;
		} else {
			gravedad = Gravedad.NINGUNA;
		}
		return new Resultado(List.copyOf(conceptos), gravedad);
	}

	/** Deteriorada, o capa con el escudo JHS despegado o perdido. */
	private static boolean danada(RevisionPiezaRequest revision) {
		return revision.resultado() == ResultadoRevision.DETERIORADA || revision.defectoCapa() != null;
	}

}
