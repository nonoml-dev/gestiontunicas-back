package es.nonomldev.gestiontunicas.tunica.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import es.nonomldev.gestiontunicas.comun.entity.BaseAuditableEntity;
import es.nonomldev.gestiontunicas.importacion.entity.Importacion;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Túnica del inventario, con sus 5 piezas fijas.
 * <p>
 * Código: número del 1 al 999 o M-XXX (monaguillo). Es único entre las túnicas que no son
 * propiedad de un hermano (índice parcial en sql/postcreacion-esquema.sql): cuando una pasa a
 * PROPIEDAD_HERMANO, su código se puede reutilizar.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tunica",
		indexes = @Index(name = "ix_tunica_medida_total", columnList = "medida_total_cm"),
		check = {
			@CheckConstraint(name = "ck_tunica_codigo", constraint = "codigo ~ '^([1-9][0-9]{0,2}|M-[0-9]{3})$'"),
			@CheckConstraint(name = "ck_tunica_dobladillo", constraint = "dobladillo_cm >= 0")
		})
public class Tunica extends BaseAuditableEntity {

	private static final String PREFIJO_MONAGUILLO = "M-";

	@Column(name = "codigo", nullable = false, length = 5)
	private String codigo;

	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "medida_cm", nullable = false)
	private Integer medidaCm;

	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "dobladillo_cm", nullable = false)
	private Integer dobladilloCm = 0;

	/** Columna generada en BD: medida + dobladillo. Es la que se usa para proponer túnicas. */
	@Generated(event = { EventType.INSERT, EventType.UPDATE })
	@Column(name = "medida_total_cm", insertable = false, updatable = false,
			columnDefinition = "smallint generated always as (medida_cm + dobladillo_cm) stored")
	private Integer medidaTotalCm;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 20)
	private EstadoTunica estado = EstadoTunica.DISPONIBLE;

	@Column(name = "fecha_alta", nullable = false)
	private LocalDate fechaAlta;

	@Column(name = "fecha_baja")
	private LocalDate fechaBaja;

	@Column(name = "motivo_baja", length = 255)
	private String motivoBaja;

	/** Fecha en que pasó a ser propiedad del hermano (fin del plazo de gracia). */
	@Column(name = "fecha_propiedad")
	private LocalDate fechaPropiedad;

	@Column(name = "observaciones", columnDefinition = "text")
	private String observaciones;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "importacion_id", foreignKey = @ForeignKey(name = "fk_tunica_importacion"))
	private Importacion importacion;

	@OneToMany(mappedBy = "tunica", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Pieza> piezas = new ArrayList<>();

	/** Crea las 5 piezas fijas en buen estado. Se llama al dar de alta la túnica. */
	public void crearPiezas() {
		piezas.clear();
		for (TipoPieza tipo : TipoPieza.values()) {
			Pieza pieza = new Pieza();
			pieza.setTipo(tipo);
			pieza.setTunica(this);
			piezas.add(pieza);
		}
	}

	public Optional<Pieza> pieza(TipoPieza tipo) {
		return piezas.stream().filter(p -> p.getTipo() == tipo).findFirst();
	}

	/** Medida total calculada en Java (antes de que la BD rellene la columna generada). */
	public int calcularMedidaTotalCm() {
		return medidaCm + (dobladilloCm == null ? 0 : dobladilloCm);
	}

	public boolean esMonaguillo() {
		return codigo != null && codigo.startsWith(PREFIJO_MONAGUILLO);
	}

	public boolean estaDisponible() {
		return estado == EstadoTunica.DISPONIBLE;
	}

	/** Sigue en el inventario (no es propiedad de un hermano). */
	public boolean enInventario() {
		return estado != EstadoTunica.PROPIEDAD_HERMANO;
	}

	public boolean encajaCon(int alturaCm, int toleranciaCm) {
		return Math.abs(calcularMedidaTotalCm() - alturaCm) <= toleranciaCm;
	}

}
