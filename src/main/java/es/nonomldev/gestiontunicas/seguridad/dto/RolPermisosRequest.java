package es.nonomldev.gestiontunicas.seguridad.dto;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

/** Permisos asignados a un rol (el Administrador edita la matriz sin tocar código). */
public record RolPermisosRequest(
		@NotNull Set<String> permisos) {
}
