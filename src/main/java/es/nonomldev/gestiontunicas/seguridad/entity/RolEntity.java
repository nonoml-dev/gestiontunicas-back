package es.nonomldev.gestiontunicas.seguridad.entity;

import java.util.HashSet;
import java.util.Set;

import es.nonomldev.gestiontunicas.comun.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Rol del usuario. Sus permisos los puede ajustar el Administrador sin tocar código. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rol",
		uniqueConstraints = @UniqueConstraint(name = "ux_rol_codigo", columnNames = "codigo"))
public class RolEntity extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(name = "codigo", nullable = false, length = 20)
	private RolCodigoEnum codigo;

	@Column(name = "nombre", nullable = false, length = 50)
	private String nombre;

	@Column(name = "descripcion", length = 255)
	private String descripcion;

	@ManyToMany
	@JoinTable(name = "rol_permiso",
			joinColumns = @JoinColumn(name = "rol_id", foreignKey = @ForeignKey(name = "fk_rol_permiso_rol")),
			inverseJoinColumns = @JoinColumn(name = "permiso_id", foreignKey = @ForeignKey(name = "fk_rol_permiso_permiso")))
	private Set<PermisoEntity> permisos = new HashSet<>();

}
