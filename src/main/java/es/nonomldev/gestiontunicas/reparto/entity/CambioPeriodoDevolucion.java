package es.nonomldev.gestiontunicas.reparto.entity;

import java.time.Instant;

import es.nonomldev.gestiontunicas.comun.entity.BaseEntity;
import es.nonomldev.gestiontunicas.seguridad.entity.Usuario;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Historial de cambios del periodo de devolución y del plazo de gracia (quién, cuándo y por qué). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cambio_periodo_devolucion")
public class CambioPeriodoDevolucion extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reparto_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cambio_periodo_reparto"))
	private Reparto reparto;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name = "inicio", column = @Column(name = "inicio_anterior", nullable = false)),
		@AttributeOverride(name = "fin", column = @Column(name = "fin_anterior", nullable = false)),
		@AttributeOverride(name = "plazoGraciaMeses", column = @Column(name = "plazo_gracia_anterior", nullable = false))
	})
	private PeriodoDevolucion anterior;

	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name = "inicio", column = @Column(name = "inicio_nuevo", nullable = false)),
		@AttributeOverride(name = "fin", column = @Column(name = "fin_nuevo", nullable = false)),
		@AttributeOverride(name = "plazoGraciaMeses", column = @Column(name = "plazo_gracia_nuevo", nullable = false))
	})
	private PeriodoDevolucion nuevo;

	@Column(name = "motivo", nullable = false, length = 255)
	private String motivo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cambio_periodo_usuario"))
	private Usuario usuario;

	@Column(name = "fecha", nullable = false)
	private Instant fecha;

}
