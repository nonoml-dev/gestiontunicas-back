package es.nonomldev.gestiontunicas.comun.entity;

import java.util.Objects;

import org.hibernate.proxy.HibernateProxy;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * Base de todas las entidades: solo el identificador interno.
 * <p>
 * La heredan directamente las entidades que no cambian una vez creadas o que ya guardan
 * su propio usuario y fecha (resguardos, revisiones, registro de inicios de sesión...).
 */
@Getter
@MappedSuperclass
public abstract class BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	/**
	 * Igualdad por id, teniendo en cuenta los proxies de Hibernate.
	 * Dos entidades nuevas (sin id) solo son iguales si son el mismo objeto.
	 */
	@Override
	public final boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || claseReal(this) != claseReal(o)) {
			return false;
		}
		Long otroId = ((BaseEntity) o).getId();
		return id != null && Objects.equals(id, otroId);
	}

	@Override
	public final int hashCode() {
		return claseReal(this).hashCode();
	}

	private static Class<?> claseReal(Object o) {
		return o instanceof HibernateProxy proxy
				? proxy.getHibernateLazyInitializer().getPersistentClass()
				: o.getClass();
	}

}
