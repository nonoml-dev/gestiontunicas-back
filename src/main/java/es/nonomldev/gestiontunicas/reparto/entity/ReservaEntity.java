package es.nonomldev.gestiontunicas.reparto.entity;

import java.time.Instant;

import es.nonomldev.gestiontunicas.alquiler.entity.AlquilerEntity;
import es.nonomldev.gestiontunicas.comun.entity.BaseAuditableEntity;
import es.nonomldev.gestiontunicas.hermano.entity.HermanoEntity;
import es.nonomldev.gestiontunicas.tunica.entity.TunicaEntity;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Túnica reservada al abrir el reparto para quien renueva.
 * Las no recogidas se liberan al empezar el día 3.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reserva",
		uniqueConstraints = {
			@UniqueConstraint(name = "ux_reserva_hermano", columnNames = { "reparto_id", "hermano_id" }),
			@UniqueConstraint(name = "ux_reserva_tunica", columnNames = { "reparto_id", "tunica_id" })
		},
		check = @CheckConstraint(name = "ck_reserva_recogida", constraint = "estado <> 'RECOGIDA' or alquiler_id is not null"))
public class ReservaEntity extends BaseAuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reparto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reserva_reparto"))
	private RepartoEntity reparto;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "hermano_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reserva_hermano"))
	private HermanoEntity hermano;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tunica_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reserva_tunica"))
	private TunicaEntity tunica;

	/** Alquiler del año anterior que da derecho a la renovación. */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "alquiler_anterior_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reserva_alquiler_anterior"))
	private AlquilerEntity alquilerAnterior;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoReservaEnum estado = EstadoReservaEnum.ACTIVA;

	/** Alquiler generado al recogerla. */
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "alquiler_id", foreignKey = @ForeignKey(name = "fk_reserva_alquiler"))
	private AlquilerEntity alquiler;

	@Column(name = "resuelta_en")
	private Instant resueltaEn;

}
