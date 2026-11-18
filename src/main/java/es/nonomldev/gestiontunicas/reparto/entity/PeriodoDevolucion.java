package es.nonomldev.gestiontunicas.reparto.entity;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Periodo de devolución del reparto y plazo de gracia posterior.
 * Tras la fecha fin, el hermano tiene {@code plazoGraciaMeses} para devolverla; si no,
 * se cobra el cargo total y la túnica pasa a ser suya.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class PeriodoDevolucion {

	public static final int PLAZO_GRACIA_MESES_POR_DEFECTO = 2;

	@Column(name = "devolucion_inicio", nullable = false)
	private LocalDate inicio;

	@Column(name = "devolucion_fin", nullable = false)
	private LocalDate fin;

	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "plazo_gracia_meses", nullable = false)
	private Integer plazoGraciaMeses = PLAZO_GRACIA_MESES_POR_DEFECTO;

	public LocalDate finPlazoGracia() {
		return fin.plusMonths(plazoGraciaMeses);
	}

	public boolean fueraDePlazo(LocalDate fecha) {
		return fecha.isAfter(fin);
	}

	public boolean enPlazoGracia(LocalDate fecha) {
		return fueraDePlazo(fecha) && !fecha.isAfter(finPlazoGracia());
	}

	public int diasRetraso(LocalDate fecha) {
		return fueraDePlazo(fecha) ? (int) ChronoUnit.DAYS.between(fin, fecha) : 0;
	}

}
