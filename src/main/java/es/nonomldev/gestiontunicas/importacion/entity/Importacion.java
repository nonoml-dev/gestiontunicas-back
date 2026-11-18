package es.nonomldev.gestiontunicas.importacion.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import es.nonomldev.gestiontunicas.comun.entity.BaseEntity;
import es.nonomldev.gestiontunicas.reparto.entity.Reparto;
import es.nonomldev.gestiontunicas.seguridad.entity.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Importación de un Excel (hermanos o túnicas). Formato fijo: no hay mapeo de columnas. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "importacion")
public class Importacion extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 10)
	private TipoImportacion tipo;

	/** Reparto en cuyo asistente se cargó el censo, si aplica. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reparto_id", foreignKey = @ForeignKey(name = "fk_importacion_reparto"))
	private Reparto reparto;

	@Column(name = "nombre_fichero", nullable = false, length = 255)
	private String nombreFichero;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 15)
	private EstadoImportacion estado = EstadoImportacion.PREVISUALIZADA;

	@Column(name = "total_filas", nullable = false)
	private int totalFilas;

	@Column(name = "filas_ok", nullable = false)
	private int filasOk;

	@Column(name = "filas_error", nullable = false)
	private int filasError;

	@Column(name = "filas_duplicadas", nullable = false)
	private int filasDuplicadas;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_importacion_usuario"))
	private Usuario usuario;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	@Column(name = "confirmado_en")
	private Instant confirmadoEn;

	@OneToMany(mappedBy = "importacion", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("numFila")
	private List<ImportacionFila> filas = new ArrayList<>();

	public void addFila(ImportacionFila fila) {
		fila.setImportacion(this);
		filas.add(fila);
	}

	public void recalcularContadores() {
		totalFilas = filas.size();
		filasOk = (int) filas.stream().filter(f -> f.getEstado() == EstadoFila.OK).count();
		filasError = (int) filas.stream().filter(f -> f.getEstado() == EstadoFila.ERROR).count();
		filasDuplicadas = (int) filas.stream().filter(f -> f.getEstado() == EstadoFila.POSIBLE_DUPLICADO).count();
	}

}
