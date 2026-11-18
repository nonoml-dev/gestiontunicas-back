package es.nonomldev.gestiontunicas.tunica.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import es.nonomldev.gestiontunicas.comun.entity.BaseAuditableEntity;
import es.nonomldev.gestiontunicas.devolucion.entity.Devolucion;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Reparación o reposición de una pieza. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reparacion",
		check = @CheckConstraint(name = "ck_reparacion_finalizada", constraint = "estado <> 'FINALIZADA' or fecha_fin is not null"))
public class Reparacion extends BaseAuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pieza_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reparacion_pieza"))
	private Pieza pieza;

	/** Devolución en la que se detectó el daño, si viene de una revisión. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "devolucion_id", foreignKey = @ForeignKey(name = "fk_reparacion_devolucion"))
	private Devolucion devolucion;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 10)
	private TipoReparacion tipo;

	@Column(name = "descripcion", nullable = false, length = 500)
	private String descripcion;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoReparacion estado = EstadoReparacion.PENDIENTE;

	@Column(name = "fecha_inicio", nullable = false)
	private LocalDate fechaInicio;

	@Column(name = "fecha_fin")
	private LocalDate fechaFin;

	@Column(name = "coste", precision = 8, scale = 2)
	private BigDecimal coste;

}
