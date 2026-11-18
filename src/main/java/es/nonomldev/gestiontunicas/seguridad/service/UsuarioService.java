package es.nonomldev.gestiontunicas.seguridad.service;

import java.util.List;

import es.nonomldev.gestiontunicas.seguridad.dto.CambioPasswordRequest;
import es.nonomldev.gestiontunicas.seguridad.dto.UsuarioCreadoDTO;
import es.nonomldev.gestiontunicas.seguridad.dto.UsuarioDTO;
import es.nonomldev.gestiontunicas.seguridad.dto.UsuarioRequest;

/** Gestión de usuarios (pantalla de Administración). */
public interface UsuarioService {

	List<UsuarioDTO> listar();

	UsuarioDTO obtener(Long id);

	/** Crea el usuario con una contraseña temporal que tendrá que cambiar en su primer acceso. */
	UsuarioCreadoDTO crear(UsuarioRequest request);

	UsuarioDTO actualizar(Long id, UsuarioRequest request);

	/** Genera una contraseña temporal nueva, desbloquea al usuario y cierra sus sesiones. */
	UsuarioCreadoDTO restablecerPassword(Long id);

	/** Cambio de contraseña del usuario autenticado. */
	void cambiarPasswordPropia(CambioPasswordRequest request);

}
