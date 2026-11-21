package es.nonomldev.gestiontunicas.seguridad.dto;

import java.util.Set;

import es.nonomldev.gestiontunicas.seguridad.entity.CargoUsuarioEnum;
import es.nonomldev.gestiontunicas.seguridad.entity.RolCodigoEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta o edición de un usuario (pantalla de Administración).
 * La contraseña no va aquí: al crear, el servicio genera una temporal y obliga a cambiarla.
 */
public record UsuarioRequest(
		@NotBlank @Size(max = 50) @Pattern(regexp = "^[a-zA-Z0-9._-]+$") String username,
		@NotBlank @Size(max = 100) String nombre,
		@Size(max = 150) String apellidos,
		@NotNull CargoUsuarioEnum cargo,
		@Email @Size(max = 150) String email,
		@NotNull Boolean activo,
		@NotEmpty Set<RolCodigoEnum> roles) {
}
