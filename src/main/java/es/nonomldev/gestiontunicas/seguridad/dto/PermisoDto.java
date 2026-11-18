package es.nonomldev.gestiontunicas.seguridad.dto;

public record PermisoDto(
		Long id,
		String codigo,
		String modulo,
		String descripcion) {
}
