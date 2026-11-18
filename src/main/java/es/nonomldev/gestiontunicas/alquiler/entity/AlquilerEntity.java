package es.nonomldev.gestiontunicas.alquiler.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import es.nonomldev.gestiontunicas.comun.entity.BaseAuditableEntity;
import es.nonomldev.gestiontunicas.hermano.entity.HermanoEntity;
import es.nonomldev.gestiontunicas.reparto.entity.JornadaEntity;
import es.nonomldev.gestiontunicas.reparto.entity.RepartoEntity;
import es.nonomldev.gestiontunicas.seguridad.entity.UsuarioEntity;
import es.nonomldev.gestiontunicas.tunica.entity.TunicaEntity;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entrega de una túnica a un hermano en un reparto (renovación, nueva alta o cambio).
 * Los índices únicos parciales (una túnica y un hermano activos) están en sql/postcreacion-esquema.sql.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "alquiler",
		indexes = @Index(name = "ix_alquiler_hermano", columnList = "hermano_id"),
		check = {
			@CheckConstraint(name = "ck_alquiler_cambio", constraint = "tipo <> 'CAMBIO' or alquiler_origen_id is not null"),
			@CheckConstraint(name = "ck_alquiler_altura", constraint = "altura_hermano_cm is null or altura_hermano_cm between 80 and 220")
		})
public class AlquilerEntity extends BaseAuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reparto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_alquiler_reparto"))
	private RepartoEntity reparto;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "hermano_id", nullable = false, foreignKey = @ForeignKey(name = "fk_alquiler_hermano"))
	private HermanoEntity hermano;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tunica_id", nullable = false, foreignKey = @ForeignKey(name = "fk_alquiler_tunica"))
	private TunicaEntity tunica;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "jornada_id", foreignKey = @ForeignKey(name = "fk_alquiler_jornada"))
	private JornadaEntity jornada;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 12)
	private TipoAlquilerEnum tipo;

	/** Modo transición 2027: renovación con el código del resguardo en papel de 2026. */
	@Column(name = "renovacion_declarada", nullable = false)
	private boolean renovacionDeclarada;

	/** Alquiler del año anterior (renovaciones). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "alquiler_anterior_id", foreignKey = @ForeignKey(name = "fk_alquiler_anterior"))
	private AlquilerEntity alquilerAnterior;

	/** Alquiler que se cambia (obligatorio si es un CAMBIO). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "alquiler_origen_id", foreignKey = @ForeignKey(name = "fk_alquiler_origen"))
	private AlquilerEntity alquilerOrigen;

	/** Altura medida al entregar. */
	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "altura_hermano_cm")
	private Integer alturaHermanoCm;

	/** Limosna aplicada (la del reparto o la de la jornada extraordinaria). */
	@Column(name = "limosna", nullable = false, precision = 8, scale = 2)
	private BigDecimal limosna;

	@Column(name = "fecha_entrega", nullable = false)
	private Instant fechaEntrega;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_entrega_id", nullable = false, foreignKey = @ForeignKey(name = "fk_alquiler_usuario_entrega"))
	private UsuarioEntity usuarioEntrega;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 21)
	private EstadoAlquilerEnum estado = EstadoAlquilerEnum.ACTIVO;

	@Column(name = "observaciones", columnDefinition = "text")
	private String observaciones;

	@OneToMany(mappedBy = "alquiler", cascade = CascadeType.ALL)
	@OrderBy("fechaEmision")
	private List<ResguardoEntity> resguardos = new ArrayList<>();

	public void addResguardo(ResguardoEntity resguardo) {
		resguardo.setAlquiler(this);
		resguardos.add(resguardo);
	}

	/** El único resguardo no anulado. */
	public Optional<ResguardoEntity> resguardoVigente() {
		return resguardos.stream().filter(r -> !r.isAnulado()).findFirst();
	}

	public boolean estaActivo() {
		return estado == EstadoAlquilerEnum.ACTIVO;
	}

}
