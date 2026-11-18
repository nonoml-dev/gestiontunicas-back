package es.nonomldev.gestiontunicas.seguridad.mapper;

import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import es.nonomldev.gestiontunicas.comun.mapper.MapStructConfig;
import es.nonomldev.gestiontunicas.seguridad.dto.UsuarioDto;
import es.nonomldev.gestiontunicas.seguridad.dto.UsuarioRequest;
import es.nonomldev.gestiontunicas.seguridad.entity.Rol;
import es.nonomldev.gestiontunicas.seguridad.entity.RolCodigo;
import es.nonomldev.gestiontunicas.seguridad.entity.Usuario;

@Mapper(config = MapStructConfig.class)
public interface UsuarioMapper {

	@Mapping(target = "cargoEtiqueta", source = "cargo.etiqueta")
	@Mapping(target = "bloqueado", expression = "java(usuario.estaBloqueado())")
	UsuarioDto toDto(Usuario usuario);

	/** Los roles, la contraseña y los datos de seguridad los rellena el servicio. */
	@Mapping(target = "roles", ignore = true)
	@Mapping(target = "passwordHash", ignore = true)
	@Mapping(target = "debeCambiarPassword", ignore = true)
	@Mapping(target = "intentosFallidos", ignore = true)
	@Mapping(target = "bloqueadoHasta", ignore = true)
	@Mapping(target = "ultimoAcceso", ignore = true)
	Usuario toEntity(UsuarioRequest request);

	@Mapping(target = "roles", ignore = true)
	@Mapping(target = "passwordHash", ignore = true)
	@Mapping(target = "debeCambiarPassword", ignore = true)
	@Mapping(target = "intentosFallidos", ignore = true)
	@Mapping(target = "bloqueadoHasta", ignore = true)
	@Mapping(target = "ultimoAcceso", ignore = true)
	void actualizar(UsuarioRequest request, @MappingTarget Usuario usuario);

	default Set<RolCodigo> rolesACodigos(Set<Rol> roles) {
		return roles.stream().map(Rol::getCodigo).collect(Collectors.toSet());
	}

}
