package es.nonomldev.gestiontunicas.seguridad.service;

import java.util.List;

import es.nonomldev.gestiontunicas.seguridad.dto.PermisoDTO;
import es.nonomldev.gestiontunicas.seguridad.dto.RolDTO;
import es.nonomldev.gestiontunicas.seguridad.dto.RolPermisosRequest;

/** Matriz de roles y permisos (Administración). */
public interface RolService {

	List<RolDTO> listar();

	List<PermisoDTO> listarPermisos();

	RolDTO actualizarPermisos(Long rolId, RolPermisosRequest request);

}
