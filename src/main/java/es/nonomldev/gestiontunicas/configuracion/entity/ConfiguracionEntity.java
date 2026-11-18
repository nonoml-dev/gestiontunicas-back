package es.nonomldev.gestiontunicas.configuracion.entity;

import java.math.BigDecimal;

import es.nonomldev.gestiontunicas.comun.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Parámetro de configuración general (valores por defecto del reparto, SMTP, seguridad...). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "configuracion",
		uniqueConstraints = @UniqueConstraint(name = "ux_configuracion_clave", columnNames = "clave"))
public class ConfiguracionEntity extends BaseAuditableEntity {

	@Column(name = "clave", nullable = false, length = 80)
	private String clave;

	@Column(name = "valor", columnDefinition = "text")
	private String valor;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 10)
	private TipoValorEnum tipo;

	@Column(name = "descripcion", length = 255)
	private String descripcion;

	public Integer comoEntero() {
		return valor == null ? null : Integer.valueOf(valor.trim());
	}

	public BigDecimal comoDecimal() {
		return valor == null ? null : new BigDecimal(valor.trim());
	}

	public boolean comoBooleano() {
		return Boolean.parseBoolean(valor);
	}

}
