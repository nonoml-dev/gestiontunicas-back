package es.nonomldev.gestiontunicas.hermano.entity;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.StringJoiner;

import es.nonomldev.gestiontunicas.comun.entity.BaseAuditableEntity;
import es.nonomldev.gestiontunicas.importacion.entity.ImportacionEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Hermano del censo. Se carga desde el Excel de Secretaría al iniciar cada reparto.
 * Se identifica por un id interno; se busca por nombre y apellidos (índice trigram en sql/postcreacion-esquema.sql).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "hermano",
		indexes = @Index(name = "ix_hermano_emparejamiento", columnList = "nombre_normalizado, fecha_nacimiento"))
public class HermanoEntity extends BaseAuditableEntity {

	/** Número de hermano del Excel. Cambia con los años: no es único ni se busca por él. */
	@Column(name = "numero_hermano", length = 10)
	private String numeroHermano;

	/**
	 * El número de hermano como entero, solo para ordenar el listado ("987" antes que "1445").
	 * Nulo si no tiene número o no es solo cifras. Se calcula al guardar.
	 */
	@Column(name = "numero_orden")
	private Integer numeroOrden;

	@Column(name = "nombre", nullable = false, length = 100)
	private String nombre;

	@Column(name = "apellidos", nullable = false, length = 150)
	private String apellidos;

	/** "apellidos nombre" sin tildes y en minúsculas, para la búsqueda. */
	@Column(name = "nombre_normalizado", nullable = false, length = 260)
	private String nombreNormalizado;

	/** Nula = ficha incompleta: no puede alquilar hasta que se complete. */
	@Column(name = "fecha_nacimiento")
	private LocalDate fechaNacimiento;

	@Column(name = "telefono1", length = 20)
	private String telefono1;

	@Column(name = "telefono2", length = 20)
	private String telefono2;

	@Column(name = "email", length = 150)
	private String email;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	/** Importación que lo creó, si vino del Excel. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "importacion_id", foreignKey = @ForeignKey(name = "fk_hermano_importacion"))
	private ImportacionEntity importacion;

	public boolean fichaCompleta() {
		return fechaNacimiento != null;
	}

	/** Edad cumplida en la fecha indicada (p. ej. el Jueves Santo del reparto). */
	public int edadA(LocalDate fecha) {
		return Period.between(fechaNacimiento, fecha).getYears();
	}

	/** Los dos teléfonos separados por " / " (variable {{hermano.telefonos}}). */
	public String telefonos() {
		StringJoiner joiner = new StringJoiner(" / ");
		for (String telefono : new String[] { telefono1, telefono2 }) {
			if (telefono != null && !telefono.isBlank()) {
				joiner.add(telefono);
			}
		}
		return joiner.toString();
	}

	public boolean tieneTelefono() {
		return !telefonos().isEmpty();
	}

	@PrePersist
	@PreUpdate
	void normalizarNombre() {
		nombreNormalizado = normalizar(apellidos + " " + nombre);
		numeroOrden = numeroComoEntero(numeroHermano);
	}

	static Integer numeroComoEntero(String numero) {
		if (numero == null) {
			return null;
		}
		String limpio = numero.trim();
		return limpio.matches("[0-9]{1,9}") ? Integer.valueOf(limpio) : null;
	}

	/** Quita tildes, pasa a minúsculas y une espacios. Se usa también para normalizar el texto buscado. */
	public static String normalizar(String texto) {
		String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return sinTildes.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
	}

}
