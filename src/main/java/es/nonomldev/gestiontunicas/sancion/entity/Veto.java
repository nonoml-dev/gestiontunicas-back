package es.nonomldev.gestiontunicas.sancion.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import es.nonomldev.gestiontunicas.alquiler.entity.Alquiler;
import es.nonomldev.gestiontunicas.comun.entity.BaseAuditableEntity;
import es.nonomldev.gestiontunicas.hermano.entity.Hermano;
import es.nonomldev.gestiontunicas.seguridad.entity.Usuario;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Veto para alquilar. Dura un solo año: el del reparto {@code anioVetado}.
 * Se conserva en el historial aunque haya pasado o se haya levantado.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "veto",
		uniqueConstraints = @UniqueConstraint(name = "ux_veto_alquiler", columnNames = "alquiler_id"),
		indexes = @Index(name = "ix_veto_hermano_anio", columnList = "hermano_id, anio_vetado"),
		check = {
			@CheckConstraint(name = "ck_veto_automatico", constraint = "origen <> 'NO_DEVOLUCION_EN_PLAZO' or alquiler_id is not null"),
			@CheckConstraint(name = "ck_veto_levantado", constraint = "activo or motivo_levantamiento is not null")
		})
public class Veto extends BaseAuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "hermano_id", nullable = false, foreignKey = @ForeignKey(name = "fk_veto_hermano"))
	private Hermano hermano;

	@Enumerated(EnumType.STRING)
	@Column(name = "origen", nullable = false, length = 22)
	private OrigenVeto origen;

	/** Año del reparto en el que no puede alquilar. */
	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "anio_vetado", nullable = false)
	private Integer anioVetado;

	/** Alquiler no devuelto en plazo (obligatorio si el veto es automático). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "alquiler_id", foreignKey = @ForeignKey(name = "fk_veto_alquiler"))
	private Alquiler alquiler;

	@Column(name = "motivo", nullable = false, length = 500)
	private String motivo;

	@Column(name = "fecha", nullable = false)
	private Instant fecha;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_veto_usuario"))
	private Usuario usuario;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	@Column(name = "fecha_levantamiento")
	private Instant fechaLevantamiento;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_levantamiento_id", foreignKey = @ForeignKey(name = "fk_veto_usuario_levantamiento"))
	private Usuario usuarioLevantamiento;

	@Column(name = "motivo_levantamiento", length = 500)
	private String motivoLevantamiento;

	/** ¿Impide alquilar en el reparto de ese año? */
	public boolean aplicaEn(int anio) {
		return activo && anioVetado == anio;
	}

	public void levantar(Usuario quien, String motivo) {
		activo = false;
		fechaLevantamiento = Instant.now();
		usuarioLevantamiento = quien;
		motivoLevantamiento = motivo;
	}

}
