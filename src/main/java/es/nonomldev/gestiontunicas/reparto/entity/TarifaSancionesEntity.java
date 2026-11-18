package es.nonomldev.gestiontunicas.reparto.entity;

import java.math.BigDecimal;

import es.nonomldev.gestiontunicas.sancion.entity.ConceptoSancionEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Importes de sanción fijados al iniciar el reparto (A–F). Se guardan en la tabla reparto. */
@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class TarifaSancionesEntity {

	/** A: túnica, capa o antifaz deteriorada o no entregada. */
	@Column(name = "importe_pieza_principal", nullable = false, precision = 8, scale = 2)
	private BigDecimal importePiezaPrincipal;

	/** B: talega no entregada. */
	@Column(name = "importe_talega", nullable = false, precision = 8, scale = 2)
	private BigDecimal importeTalega;

	/** C: no devuelta en plazo. */
	@Column(name = "importe_retraso", nullable = false, precision = 8, scale = 2)
	private BigDecimal importeRetraso;

	/** D: cíngulo no entregado o deteriorado. */
	@Column(name = "importe_cingulo", nullable = false, precision = 8, scale = 2)
	private BigDecimal importeCingulo;

	/** E: devuelta sucia. */
	@Column(name = "importe_suciedad", nullable = false, precision = 8, scale = 2)
	private BigDecimal importeSuciedad;

	/** F: cargo total de la túnica (no devuelta al terminar el plazo de gracia). */
	@Column(name = "importe_cargo_total", nullable = false, precision = 8, scale = 2)
	private BigDecimal importeCargoTotal;

	public BigDecimal importe(ConceptoSancionEnum concepto) {
		return switch (concepto) {
			case PIEZA_PRINCIPAL -> importePiezaPrincipal;
			case TALEGA -> importeTalega;
			case RETRASO -> importeRetraso;
			case CINGULO -> importeCingulo;
			case SUCIEDAD -> importeSuciedad;
			case CARGO_TOTAL -> importeCargoTotal;
		};
	}

}
