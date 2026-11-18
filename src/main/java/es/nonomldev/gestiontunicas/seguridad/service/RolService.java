package es.nonomldev.gestiontunicas.seguridad.service;

import java.util.List;

import es.nonomldev.gestiontunicas.seguridad.dto.PermisoDto;
import es.nonomldev.gestiontunicas.seguridad.dto.RolDto;
import es.nonomldev.gestiontunicas.seguridad.dto.RolPermisosRequest;

/** Matriz de roles y permisos (Administración). */
public interface RolService {

	List<RolDto> listar();

	List<PermisoDto> listarPermisos();

	RolDto actualizarPermisos(Long rolId, RolPermisosRequest request);

}
