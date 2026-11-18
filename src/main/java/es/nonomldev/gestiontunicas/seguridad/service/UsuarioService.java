package es.nonomldev.gestiontunicas.seguridad.service;

import java.util.List;

import es.nonomldev.gestiontunicas.seguridad.dto.CambioPasswordRequest;
import es.nonomldev.gestiontunicas.seguridad.dto.UsuarioCreadoDto;
import es.nonomldev.gestiontunicas.seguridad.dto.UsuarioDto;
import es.nonomldev.gestiontunicas.seguridad.dto.UsuarioRequest;

/** Gestión de usuarios (pantalla de Administración). */
public interface UsuarioService {

	List<UsuarioDto> listar();

	UsuarioDto obtener(Long id);

	/** Crea el usuario con una contraseña temporal que tendrá que cambiar en su primer acceso. */
	UsuarioCreadoDto crear(UsuarioRequest request);

	UsuarioDto actualizar(Long id, UsuarioRequest request);

	/** Genera una contraseña temporal nueva, desbloquea al usuario y cierra sus sesiones. */
	UsuarioCreadoDto restablecerPassword(Long id);

	/** Cambio de contraseña del usuario autenticado. */
	void cambiarPasswordPropia(CambioPasswordRequest request);

}
