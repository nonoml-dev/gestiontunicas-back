package es.nonomldev.gestiontunicas.seguridad.mapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.seguridad.dto.PermisoDto;
import es.nonomldev.gestiontunicas.seguridad.dto.RolDto;
import es.nonomldev.gestiontunicas.seguridad.entity.Permiso;
import es.nonomldev.gestiontunicas.seguridad.entity.Rol;

@Mapper(config = MapStructConfig.class)
public interface RolMapper {

	RolDto toDto(Rol rol);

	List<RolDto> toDtos(List<Rol> roles);

	PermisoDto toDto(Permiso permiso);

	List<PermisoDto> toPermisoDtos(List<Permiso> permisos);

	default Set<String> permisosACodigos(Set<Permiso> permisos) {
		return permisos.stream().map(Permiso::getCodigo).collect(Collectors.toSet());
	}

}
