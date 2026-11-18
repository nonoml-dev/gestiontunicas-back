package es.nonomldev.gestiontunicas.importacion.dto;

import es.nonomldev.gestiontunicas.importacion.entity.DecisionFilaEnum;
import jakarta.validation.constraints.NotNull;

/** Decisión sobre un posible duplicado: actualizar el existente, crear uno nuevo o ignorar la fila. */
public record DecisionFilaRequest(
		@NotNull DecisionFilaEnum decision) {
}
