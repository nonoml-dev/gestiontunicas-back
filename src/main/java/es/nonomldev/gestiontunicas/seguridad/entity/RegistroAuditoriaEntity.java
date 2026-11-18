package es.nonomldev.gestiontunicas.seguridad.entity;

import java.time.Instant;

import es.nonomldev.gestiontunicas.comun.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Registro de inicios de sesión (correctos, fallidos y bloqueados). Solo lo consulta el Administrador. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "registro_auditoria",
		indexes = @Index(name = "ix_registro_auditoria_fecha", columnList = "fecha"))
public class RegistroAuditoriaEntity extends BaseEntity {

	@Column(name = "fecha", nullable = false)
	private Instant fecha;

	/** Nulo si el nombre de usuario introducido no existe. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id", foreignKey = @ForeignKey(name = "fk_registro_auditoria_usuario"))
	private UsuarioEntity usuario;

	/** Nombre de usuario tal y como se introdujo. */
	@Column(name = "username", nullable = false, length = 50)
	private String username;

	@Enumerated(EnumType.STRING)
	@Column(name = "resultado", nullable = false, length = 10)
	private ResultadoAccesoEnum resultado;

	@Column(name = "ip", length = 45)
	private String ip;

}
