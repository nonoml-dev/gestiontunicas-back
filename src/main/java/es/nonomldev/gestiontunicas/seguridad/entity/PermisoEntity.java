package es.nonomldev.gestiontunicas.seguridad.entity;

import es.nonomldev.gestiontunicas.comun.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Permiso atómico (p. ej. "hermanos.importar"). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "permiso",
		uniqueConstraints = @UniqueConstraint(name = "ux_permiso_codigo", columnNames = "codigo"))
public class PermisoEntity extends BaseEntity {

	@Column(name = "codigo", nullable = false, length = 50)
	private String codigo;

	@Column(name = "modulo", nullable = false, length = 30)
	private String modulo;

	@Column(name = "descripcion", nullable = false, length = 255)
	private String descripcion;

}
