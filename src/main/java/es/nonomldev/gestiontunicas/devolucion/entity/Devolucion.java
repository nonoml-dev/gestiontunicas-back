package es.nonomldev.gestiontunicas.devolucion.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import es.nonomldev.gestiontunicas.alquiler.entity.Alquiler;
import es.nonomldev.gestiontunicas.comun.entity.BaseAuditableEntity;
import es.nonomldev.gestiontunicas.seguridad.entity.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Devolución de una túnica, con la revisión pieza a pieza. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "devolucion",
		uniqueConstraints = @UniqueConstraint(name = "ux_devolucion_alquiler", columnNames = "alquiler_id"),
		check = @CheckConstraint(name = "ck_devolucion_dias_retraso", constraint = "dias_retraso >= 0"))
public class Devolucion extends BaseAuditableEntity {

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "alquiler_id", nullable = false, foreignKey = @ForeignKey(name = "fk_devolucion_alquiler"))
	private Alquiler alquiler;

	@Column(name = "fecha", nullable = false)
	private Instant fecha;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_devolucion_usuario"))
	private Usuario usuario;

	/** Devolución rápida al hacer un cambio de túnica. */
	@Column(name = "por_cambio", nullable = false)
	private boolean porCambio;

	@Column(name = "fuera_plazo", nullable = false)
	private boolean fueraPlazo;

	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "dias_retraso", nullable = false)
	private Integer diasRetraso = 0;

	@Enumerated(EnumType.STRING)
	@Column(name = "gravedad", nullable = false, length = 10)
	private Gravedad gravedad = Gravedad.NINGUNA;

	@Column(name = "observaciones", columnDefinition = "text")
	private String observaciones;

	@OneToMany(mappedBy = "devolucion", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<RevisionPieza> revisiones = new ArrayList<>();

	public void addRevision(RevisionPieza revision) {
		revision.setDevolucion(this);
		revisiones.add(revision);
	}

	public List<RevisionPieza> piezasConIncidencia() {
		return revisiones.stream().filter(RevisionPieza::conIncidencia).toList();
	}

}
