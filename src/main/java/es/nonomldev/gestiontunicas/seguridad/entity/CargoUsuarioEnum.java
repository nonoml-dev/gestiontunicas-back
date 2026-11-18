package es.nonomldev.gestiontunicas.seguridad.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Cargo del usuario en la Junta. Es lo que se muestra; los permisos los dan los roles. */
@Getter
@RequiredArgsConstructor
public enum CargoUsuarioEnum {

	PRIOSTE_PRIMERO("Prioste primero"),
	PRIOSTE_SEGUNDO("Prioste segundo"),
	MAYORDOMO("Mayordomo"),
	SECRETARIO("Secretario"),
	CAPILLER("Capiller"),
	ADMINISTRADOR("Administrador");

	private final String etiqueta;

}
