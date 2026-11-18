package es.nonomldev.gestiontunicas.reparto.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import es.nonomldev.gestiontunicas.comun.entity.BaseEntity;
import es.nonomldev.gestiontunicas.seguridad.entity.Usuario;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Plantilla Word (.docx) del resguardo que sube el prioste.
 * No se edita: subir otra crea un registro nuevo y los resguardos guardan con cuál se generaron.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "plantilla_resguardo")
public class PlantillaResguardo extends BaseEntity {

	@Column(name = "nombre", nullable = false, length = 100)
	private String nombre;

	@Column(name = "nombre_fichero", nullable = false, length = 255)
	private String nombreFichero;

	/** Contenido del .docx. */
	@Basic(fetch = FetchType.LAZY)
	@Column(name = "contenido", nullable = false, columnDefinition = "bytea")
	private byte[] contenido;

	/** Variables {{...}} encontradas en el Word al subirlo. */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "variables_detectadas", nullable = false)
	private List<String> variablesDetectadas = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subido_por", nullable = false, foreignKey = @ForeignKey(name = "fk_plantilla_resguardo_usuario"))
	private Usuario subidoPor;

	@Column(name = "subido_en", nullable = false)
	private Instant subidoEn;

}
