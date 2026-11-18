package es.nonomldev.gestiontunicas.seguridad.dto;

import java.time.Instant;
import java.util.Set;

import es.nonomldev.gestiontunicas.seguridad.entity.CargoUsuario;
import es.nonomldev.gestiontunicas.seguridad.entity.RolCodigo;

/** Usuario tal y como se devuelve por la API (nunca incluye la contraseña). */
public record UsuarioDto(
		Long id,
		String username,
		String nombre,
		String apellidos,
		CargoUsuario cargo,
		String cargoEtiqueta,
		String email,
		boolean activo,
		boolean debeCambiarPassword,
		boolean bloqueado,
		Instant ultimoAcceso,
		Set<RolCodigo> roles) {
}
