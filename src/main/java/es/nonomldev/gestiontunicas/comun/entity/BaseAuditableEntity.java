package es.nonomldev.gestiontunicas.comun.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;

/**
 * Base de las entidades que se editan después de crearse.
 * <p>
 * Spring Data JPA Auditing rellena quién y cuándo se creó y se modificó por última vez.
 * {@code version} sirve para el bloqueo optimista (dos priostes a la vez).
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditableEntity extends BaseEntity {

	@CreatedDate
	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	@CreatedBy
	@Column(name = "creado_por", length = 50, updatable = false)
	private String creadoPor;

	@LastModifiedDate
	@Column(name = "modificado_en")
	private Instant modificadoEn;

	@LastModifiedBy
	@Column(name = "modificado_por", length = 50)
	private String modificadoPor;

	@Version
	@Column(name = "version", nullable = false)
	private Integer version;

}
