package es.nonomldev.gestiontunicas.alquiler.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import es.nonomldev.gestiontunicas.comun.entity.BaseEntity;
import es.nonomldev.gestiontunicas.reparto.entity.PlantillaResguardoEntity;
import es.nonomldev.gestiontunicas.reparto.entity.RepartoEntity;
import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;
import jakarta.persistence.CheckConstraint;
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

/**
 * Resguardo impreso de un alquiler. Guarda con qué plantilla se generó para reimprimirlo igual.
 * No se edita: en un cambio se anula y se emite otro (un solo vigente por alquiler: índice parcial en SQL).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "resguardo",
		uniqueConstraints = {
			@UniqueConstraint(name = "ux_resguardo_numero", columnNames = { "reparto_id", "numero" }),
			@UniqueConstraint(name = "ux_resguardo_codigo", columnNames = "codigo")
		},
		check = @CheckConstraint(name = "ck_resguardo_anulado", constraint = "not anulado or fecha_anulacion is not null"))
public class ResguardoEntity extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "alquiler_id", nullable = false, foreignKey = @ForeignKey(name = "fk_resguardo_alquiler"))
	private AlquilerEntity alquiler;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reparto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_resguardo_reparto"))
	private RepartoEntity reparto;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "plantilla_resguardo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_resguardo_plantilla"))
	private PlantillaResguardoEntity plantillaResguardo;

	/** Correlativo dentro del reparto. */
	@Column(name = "numero", nullable = false)
	private Integer numero;

	/** Número visible, p. ej. "2027-0042". */
	@Column(name = "codigo", nullable = false, length = 20)
	private String codigo;

	/** Primera emisión: no cambia al reimprimir (variable {{resguardo.fecha_emision}}). */
	@Column(name = "fecha_emision", nullable = false)
	private Instant fechaEmision;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_resguardo_usuario"))
	private UsuarioEntity usuario;

	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "num_impresiones", nullable = false)
	private Integer numImpresiones = 0;

	@Column(name = "anulado", nullable = false)
	private boolean anulado;

	@Column(name = "fecha_anulacion")
	private Instant fechaAnulacion;

	@Column(name = "motivo_anulacion", length = 255)
	private String motivoAnulacion;

	public static String formatearCodigo(int anio, int numero) {
		return "%d-%04d".formatted(anio, numero);
	}

	public void registrarImpresion() {
		numImpresiones++;
	}

	public void anular(String motivo) {
		anulado = true;
		fechaAnulacion = Instant.now();
		motivoAnulacion = motivo;
	}

}
