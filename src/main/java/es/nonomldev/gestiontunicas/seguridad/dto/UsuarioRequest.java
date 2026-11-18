package es.nonomldev.gestiontunicas.seguridad.dto;

import java.util.Set;

import es.nonomldev.gestiontunicas.seguridad.entity.CargoUsuario;
import es.nonomldev.gestiontunicas.seguridad.entity.RolCodigo;
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
		@NotNull CargoUsuario cargo,
		@Email @Size(max = 150) String email,
		boolean activo,
		@NotEmpty Set<RolCodigo> roles) {
}
