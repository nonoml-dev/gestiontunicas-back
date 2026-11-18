package es.nonomldev.gestiontunicas.sancion.entity;

import java.math.BigDecimal;
import java.time.Instant;

import es.nonomldev.gestiontunicas.alquiler.entity.AlquilerEntity;
import es.nonomldev.gestiontunicas.comun.entity.BaseAuditableEntity;
import es.nonomldev.gestiontunicas.devolucion.entity.DevolucionEntity;
import es.nonomldev.gestiontunicas.hermano.entity.HermanoEntity;
import es.nonomldev.gestiontunicas.reparto.entity.RepartoEntity;
import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;
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

/**
 * Sanción económica. Mientras esté PENDIENTE, el hermano no puede alquilar.
 * No se borra: se paga, se condona o se anula con motivo.
 * Cada concepto se cobra una vez por alquiler (índice parcial en sql/postcreacion-esquema.sql).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sancion",
		check = {
			@CheckConstraint(name = "ck_sancion_ajuste", constraint = "importe = importe_calculado or motivo_ajuste is not null"),
			@CheckConstraint(name = "ck_sancion_resolucion", constraint = "estado not in ('CONDONADA', 'ANULADA') or motivo_resolucion is not null")
		})
public class SancionEntity extends BaseAuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "hermano_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sancion_hermano"))
	private HermanoEntity hermano;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reparto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sancion_reparto"))
	private RepartoEntity reparto;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "alquiler_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sancion_alquiler"))
	private AlquilerEntity alquiler;

	/** Nula en RETRASO generado al cerrar el plazo y en CARGO_TOTAL. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "devolucion_id", foreignKey = @ForeignKey(name = "fk_sancion_devolucion"))
	private DevolucionEntity devolucion;

	@Enumerated(EnumType.STRING)
	@Column(name = "concepto", nullable = false, length = 16)
	private ConceptoSancionEnum concepto;

	@Column(name = "importe_calculado", nullable = false, precision = 8, scale = 2)
	private BigDecimal importeCalculado;

	@Column(name = "importe", nullable = false, precision = 8, scale = 2)
	private BigDecimal importe;

	/** Obligatorio si el importe difiere del calculado. */
	@Column(name = "motivo_ajuste", length = 255)
	private String motivoAjuste;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoSancionEnum estado = EstadoSancionEnum.PENDIENTE;

	@Column(name = "fecha_resolucion")
	private Instant fechaResolucion;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_resolucion_id", foreignKey = @ForeignKey(name = "fk_sancion_usuario_resolucion"))
	private UsuarioEntity usuarioResolucion;

	/** Obligatorio si se condona o se anula. */
	@Column(name = "motivo_resolucion", length = 255)
	private String motivoResolucion;

	public boolean estaPendiente() {
		return estado == EstadoSancionEnum.PENDIENTE;
	}

	public boolean importeAjustado() {
		return importe != null && importeCalculado != null && importe.compareTo(importeCalculado) != 0;
	}

}
