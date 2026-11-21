package es.nonomldev.gestiontunicas.seguridad.dto;

import java.util.Set;

import es.nonomldev.gestiontunicas.seguridad.entity.RolCodigoEnum;

/** Rol con los códigos de sus permisos (matriz de permisos). */
public record RolDTO(
		Long id,
		RolCodigoEnum codigo,
		String nombre,
		String descripcion,
		Set<String> permisos) {
}
