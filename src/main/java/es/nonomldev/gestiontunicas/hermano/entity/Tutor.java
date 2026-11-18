package es.nonomldev.gestiontunicas.hermano.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Datos del tutor del hermano (menor de edad). Se guardan en la tabla hermano. */
@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class Tutor {

	@Column(name = "tutor_nombre", length = 200)
	private String nombre;

	@Column(name = "tutor_telefono", length = 20)
	private String telefono;

	@Column(name = "tutor_email", length = 150)
	private String email;

}
