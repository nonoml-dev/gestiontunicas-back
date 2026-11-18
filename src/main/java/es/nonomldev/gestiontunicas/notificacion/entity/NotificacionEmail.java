package es.nonomldev.gestiontunicas.notificacion.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import es.nonomldev.gestiontunicas.alquiler.entity.Alquiler;
import es.nonomldev.gestiontunicas.comun.entity.BaseEntity;
import es.nonomldev.gestiontunicas.hermano.entity.Hermano;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Email enviado (o pendiente) a un hermano o su tutor: resguardo o recordatorio de devolución. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notificacion_email")
public class NotificacionEmail extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "hermano_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notificacion_hermano"))
	private Hermano hermano;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "alquiler_id", foreignKey = @ForeignKey(name = "fk_notificacion_alquiler"))
	private Alquiler alquiler;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 25)
	private TipoNotificacion tipo;

	@Column(name = "destinatario", nullable = false, length = 150)
	private String destinatario;

	@Column(name = "asunto", nullable = false, length = 255)
	private String asunto;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 10)
	private EstadoNotificacion estado = EstadoNotificacion.PENDIENTE;

	@JdbcTypeCode(SqlTypes.SMALLINT)
	@Column(name = "intentos", nullable = false)
	private Integer intentos = 0;

	@Column(name = "error", columnDefinition = "text")
	private String error;

	@Column(name = "creado_en", nullable = false)
	private Instant creadoEn;

	@Column(name = "enviado_en")
	private Instant enviadoEn;

	public void marcarEnviado() {
		estado = EstadoNotificacion.ENVIADO;
		enviadoEn = Instant.now();
		error = null;
	}

	public void marcarError(String mensaje) {
		estado = EstadoNotificacion.ERROR;
		intentos++;
		error = mensaje;
	}

}
