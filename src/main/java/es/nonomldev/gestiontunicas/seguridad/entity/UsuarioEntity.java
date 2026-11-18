package es.nonomldev.gestiontunicas.seguridad.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import es.nonomldev.gestiontunicas.comun.entity.BaseAuditableEntity;
import jakarta.persistence.CheckConstraint;
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

/** Usuario de la Junta que accede a la aplicación. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "usuario",
		uniqueConstraints = @UniqueConstraint(name = "ux_usuario_username", columnNames = "username"),
		check = @CheckConstraint(name = "ck_usuario_intentos", constraint = "intentos_fallidos >= 0"))
public class UsuarioEntity extends BaseAuditableEntity {

	@Column(name = "username", nullable = false, length = 50)
	private String username;

	/** Contraseña cifrada con BCrypt. */
	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "nombre", nullable = false, length = 100)
	private String nombre;

	@Column(name = "apellidos", length = 150)
	private String apellidos;

	@Enumerated(EnumType.STRING)
	@Column(name = "cargo", nullable = false, length = 20)
	private CargoUsuarioEnum cargo;

	@Column(name = "email", length = 150)
	private String email;

	@Column(name = "activo", nullable = false)
	private boolean activo = true;

	/** Obliga a cambiar la contraseña en el primer acceso. */
	@Column(name = "debe_cambiar_password", nullable = false)
	private boolean debeCambiarPassword = true;

	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "intentos_fallidos", nullable = false)
	private int intentosFallidos;

	@Column(name = "bloqueado_hasta")
	private Instant bloqueadoHasta;

	@Column(name = "ultimo_acceso")
	private Instant ultimoAcceso;

	@ManyToMany
	@JoinTable(name = "usuario_rol",
			joinColumns = @JoinColumn(name = "usuario_id", foreignKey = @ForeignKey(name = "fk_usuario_rol_usuario")),
			inverseJoinColumns = @JoinColumn(name = "rol_id", foreignKey = @ForeignKey(name = "fk_usuario_rol_rol")))
	private Set<RolEntity> roles = new HashSet<>();

	public boolean estaBloqueado() {
		return bloqueadoHasta != null && bloqueadoHasta.isAfter(Instant.now());
	}

	public boolean tienePermiso(String codigoPermiso) {
		return roles.stream()
				.flatMap(rol -> rol.getPermisos().stream())
				.anyMatch(permiso -> permiso.getCodigo().equals(codigoPermiso));
	}

}
