package es.nonomldev.gestiontunicas.seguridad.entity;

import java.time.Instant;

import es.nonomldev.gestiontunicas.comun.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sesión abierta de un usuario. Solo se guarda el hash del token. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "refresh_token",
		uniqueConstraints = @UniqueConstraint(name = "ux_refresh_token_hash", columnNames = "token_hash"),
		indexes = @Index(name = "ix_refresh_token_usuario", columnList = "usuario_id"))
public class RefreshTokenEntity extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_refresh_token_usuario"))
	private UsuarioEntity usuario;

	@Column(name = "token_hash", nullable = false, length = 100)
	private String tokenHash;

	@Column(name = "expira_en", nullable = false)
	private Instant expiraEn;

	@Column(name = "revocado", nullable = false)
	private boolean revocado;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	public boolean esValido() {
		return !revocado && expiraEn.isAfter(Instant.now());
	}

}
