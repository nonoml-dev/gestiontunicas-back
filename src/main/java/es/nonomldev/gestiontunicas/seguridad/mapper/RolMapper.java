package es.nonomldev.gestiontunicas.seguridad.mapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.seguridad.dto.PermisoDTO;
import es.nonomldev.gestiontunicas.seguridad.dto.RolDTO;
import es.nonomldev.gestiontunicas.seguridad.entity.PermisoEntity;
import es.nonomldev.gestiontunicas.seguridad.entity.RolEntity;

@Mapper(config = MapStructConfig.class)
public interface RolMapper {

	RolDTO toDTO(RolEntity rol);

	List<RolDTO> toDTOs(List<RolEntity> roles);

	PermisoDTO toDTO(PermisoEntity permiso);

	List<PermisoDTO> toPermisoDTOs(List<PermisoEntity> permisos);

	default Set<String> permisosACodigos(Set<PermisoEntity> permisos) {
		return permisos.stream().map(PermisoEntity::getCodigo).collect(Collectors.toSet());
	}

}
