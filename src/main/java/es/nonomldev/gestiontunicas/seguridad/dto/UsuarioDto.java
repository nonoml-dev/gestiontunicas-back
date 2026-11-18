package es.nonomldev.gestiontunicas.seguridad.dto;

import java.time.Instant;
import java.util.Set;

import es.nonomldev.gestiontunicas.seguridad.entity.CargoUsuarioEnum;
import es.nonomldev.gestiontunicas.seguridad.entity.RolCodigoEnum;

/** Usuario tal y como se devuelve por la API (nunca incluye la contraseña). */
public record UsuarioDTO(
		Long id,
		String username,
		String nombre,
		String apellidos,
		CargoUsuarioEnum cargo,
		String cargoEtiqueta,
		String email,
		boolean activo,
		boolean debeCambiarPassword,
		boolean bloqueado,
		Instant ultimoAcceso,
		Set<RolCodigoEnum> roles) {
}
