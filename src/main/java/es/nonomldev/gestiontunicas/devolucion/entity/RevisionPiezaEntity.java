package es.nonomldev.gestiontunicas.devolucion.entity;

import es.nonomldev.gestiontunicas.comun.entity.BaseEntity;
import es.nonomldev.gestiontunicas.tunica.entity.PiezaEntity;
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

/** Resultado de la revisión de una pieza en una devolución. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "revision_pieza",
		uniqueConstraints = @UniqueConstraint(name = "ux_revision_pieza", columnNames = { "devolucion_id", "pieza_id" }))
public class RevisionPiezaEntity extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "devolucion_id", nullable = false, foreignKey = @ForeignKey(name = "fk_revision_pieza_devolucion"))
	private DevolucionEntity devolucion;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "pieza_id", nullable = false, foreignKey = @ForeignKey(name = "fk_revision_pieza_pieza"))
	private PiezaEntity pieza;

	@Enumerated(EnumType.STRING)
	@Column(name = "resultado", nullable = false, length = 15)
	private ResultadoRevisionEnum resultado;

	/** Solo en la capa. Si se marca, la capa cuenta como deteriorada. */
	@Enumerated(EnumType.STRING)
	@Column(name = "defecto_capa", length = 15)
	private DefectoCapaEnum defectoCapa;

	@Column(name = "observaciones", columnDefinition = "text")
	private String observaciones;

	public boolean conIncidencia() {
		return resultado != ResultadoRevisionEnum.CORRECTA || defectoCapa != null;
	}

}
