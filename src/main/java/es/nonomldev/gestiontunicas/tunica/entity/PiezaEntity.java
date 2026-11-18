package es.nonomldev.gestiontunicas.tunica.entity;

import es.nonomldev.gestiontunicas.comun.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Pieza de una túnica. Pertenece siempre a la misma túnica: no se intercambian. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "pieza",
		uniqueConstraints = @UniqueConstraint(name = "ux_pieza_tipo", columnNames = { "tunica_id", "tipo" }))
public class PiezaEntity extends BaseAuditableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tunica_id", nullable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_pieza_tunica"))
	private TunicaEntity tunica;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 10, updatable = false)
	private TipoPiezaEnum tipo;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 15)
	private EstadoPiezaEnum estado = EstadoPiezaEnum.BUEN_ESTADO;

	@Column(name = "observaciones", columnDefinition = "text")
	private String observaciones;

	public boolean esPrincipal() {
		return tipo.esPrincipal();
	}

}
