package es.nonomldev.gestiontunicas.seguridad.dto;

import java.util.Set;

import es.nonomldev.gestiontunicas.seguridad.entity.RolCodigo;

/** Rol con los códigos de sus permisos (matriz de permisos). */
public record RolDto(
		Long id,
		RolCodigo codigo,
		String nombre,
		String descripcion,
		Set<String> permisos) {
}
