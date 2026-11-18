package es.nonomldev.gestiontunicas.importacion.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import es.nonomldev.gestiontunicas.comun.entity.BaseEntity;
import es.nonomldev.gestiontunicas.hermano.entity.HermanoEntity;
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

/** Fila leída del Excel, con su validación y la decisión del usuario. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "importacion_fila",
		uniqueConstraints = @UniqueConstraint(name = "ux_importacion_fila", columnNames = { "importacion_id", "num_fila" }))
public class ImportacionFilaEntity extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "importacion_id", nullable = false, foreignKey = @ForeignKey(name = "fk_importacion_fila_importacion"))
	private ImportacionEntity importacion;

	@Column(name = "num_fila", nullable = false)
	private int numFila;

	/** Datos ya interpretados (apellidos y nombre separados, número de hermano, fecha...). */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "datos", nullable = false)
	private Map<String, Object> datos = new HashMap<>();

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 20)
	private EstadoFilaEnum estado;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "errores")
	private List<String> errores = new ArrayList<>();

	/** Hermano existente con el que coincide (mismo nombre, apellidos y fecha de nacimiento). */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "hermano_coincidente_id", foreignKey = @ForeignKey(name = "fk_importacion_fila_hermano"))
	private HermanoEntity hermanoCoincidente;

	@Enumerated(EnumType.STRING)
	@Column(name = "decision", length = 10)
	private DecisionFilaEnum decision;

	/** Id del hermano o túnica creado o actualizado al confirmar. */
	@Column(name = "entidad_resultante_id")
	private Long entidadResultanteId;

}
