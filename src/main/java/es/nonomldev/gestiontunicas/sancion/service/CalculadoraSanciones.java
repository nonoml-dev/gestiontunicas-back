package es.nonomldev.gestiontunicas.sancion.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import es.nonomldev.gestiontunicas.devolucion.dto.RevisionPiezaRequest;
import es.nonomldev.gestiontunicas.devolucion.entity.GravedadEnum;
import es.nonomldev.gestiontunicas.devolucion.entity.ResultadoRevisionEnum;
import es.nonomldev.gestiontunicas.sancion.entity.ConceptoSancionEnum;
import es.nonomldev.gestiontunicas.tunica.entity.TipoPiezaEnum;

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

	public record Resultado(List<ConceptoSancionEnum> conceptos, GravedadEnum gravedad) {
	}

	public Resultado calcular(List<RevisionPiezaRequest> revisiones, boolean fueraPlazo) {
		List<ConceptoSancionEnum> conceptos = new ArrayList<>();

		boolean principalDanada = revisiones.stream()
				.filter(r -> r.tipoPieza().esPrincipal())
				.anyMatch(r -> danada(r) || r.resultado() == ResultadoRevisionEnum.NO_ENTREGADA);
		if (principalDanada) {
			conceptos.add(ConceptoSancionEnum.PIEZA_PRINCIPAL);
		}
		if (revisiones.stream().anyMatch(r -> r.tipoPieza() == TipoPiezaEnum.TALEGA
				&& r.resultado() == ResultadoRevisionEnum.NO_ENTREGADA)) {
			conceptos.add(ConceptoSancionEnum.TALEGA);
		}
		if (revisiones.stream().anyMatch(r -> r.tipoPieza() == TipoPiezaEnum.CINGULO
				&& (danada(r) || r.resultado() == ResultadoRevisionEnum.NO_ENTREGADA))) {
			conceptos.add(ConceptoSancionEnum.CINGULO);
		}
		boolean sucia = revisiones.stream().anyMatch(r -> r.resultado() == ResultadoRevisionEnum.SUCIA);
		if (sucia) {
			conceptos.add(ConceptoSancionEnum.SUCIEDAD);
		}
		if (fueraPlazo) {
			conceptos.add(ConceptoSancionEnum.RETRASO);
		}

		GravedadEnum gravedad;
		if (fueraPlazo) {
			gravedad = GravedadEnum.MUY_GRAVE;
		} else if (conceptos.stream().anyMatch(c -> c != ConceptoSancionEnum.SUCIEDAD)) {
			gravedad = GravedadEnum.GRAVE;
		} else if (sucia) {
			gravedad = GravedadEnum.LEVE;
		} else {
			gravedad = GravedadEnum.NINGUNA;
		}
		return new Resultado(List.copyOf(conceptos), gravedad);
	}

	/** Deteriorada, o capa con el escudo JHS despegado o perdido. */
	private static boolean danada(RevisionPiezaRequest revision) {
		return revision.resultado() == ResultadoRevisionEnum.DETERIORADA || revision.defectoCapa() != null;
	}

}
