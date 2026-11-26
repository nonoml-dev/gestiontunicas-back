package es.nonomldev.gestiontunicas.reparto.dto;

import jakarta.validation.constraints.NotNull;

/** Plantilla Word del resguardo que usará el reparto a partir de ahora. */
public record AsignarPlantillaRequest(
		@NotNull Long plantillaId) {
}
