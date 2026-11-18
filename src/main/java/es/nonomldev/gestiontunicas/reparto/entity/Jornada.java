package es.nonomldev.gestiontunicas.reparto.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import es.nonomldev.gestiontunicas.alquiler.entity.TipoAlquiler;
import es.nonomldev.gestiontunicas.comun.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Jornada de reparto: las 3 ordinarias y las extraordinarias que se habiliten. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "jornada",
		uniqueConstraints = @UniqueConstraint(name = "ux_jornada_numero", columnNames = { "reparto_id", "numero" }))
public class Jornada extends BaseAuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reparto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_jornada_reparto"))
	private Reparto reparto;

	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "numero", nullable = false)
	private Integer numero;

	@Column(name = "extraordinaria", nullable = false)
	private boolean extraordinaria;

	@Column(name = "fecha", nullable = false)
	private LocalDate fecha;

	@Column(name = "hora_inicio")
	private LocalTime horaInicio;

	@Column(name = "hora_fin")
	private LocalTime horaFin;

	/** Solo en jornadas extraordinarias. Nula = se aplica la limosna del reparto. */
	@Column(name = "limosna", precision = 8, scale = 2)
	private BigDecimal limosna;

	@Column(name = "permite_renovacion", nullable = false)
	private boolean permiteRenovacion;

	@Column(name = "permite_nueva_alta", nullable = false)
	private boolean permiteNuevaAlta;

	@Column(name = "permite_cambio", nullable = false)
	private boolean permiteCambio;

	public boolean permite(TipoAlquiler tipo) {
		return switch (tipo) {
			case RENOVACION -> permiteRenovacion;
			case NUEVA_ALTA -> permiteNuevaAlta;
			case CAMBIO -> permiteCambio;
		};
	}

	public BigDecimal limosnaAplicable(BigDecimal limosnaReparto) {
		return limosna != null ? limosna : limosnaReparto;
	}

}
