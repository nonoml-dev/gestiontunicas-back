package es.nonomldev.gestiontunicas.seguridad.service;

import es.nonomldev.gestiontunicas.seguridad.dto.LoginDTO;
import es.nonomldev.gestiontunicas.seguridad.dto.LoginRequest;

/** Inicio, renovación y cierre de sesión de los usuarios de la Junta. */
public interface AuthService {

	/**
	 * Comprueba las credenciales, registra el intento en el registro de inicios de sesión y abre la sesión.
	 * Tras varios intentos fallidos seguidos el usuario queda bloqueado unos minutos.
	 *
	 * @throws es.nonomldev.gestiontunicas.comun.exception.CredencialesInvalidasException si se rechaza
	 */
	LoginDTO login(LoginRequest request, String ip);

	/**
	 * Cambia un refresh token válido por un token de acceso y un refresh token nuevos (el usado queda revocado).
	 *
	 * @throws es.nonomldev.gestiontunicas.comun.exception.CredencialesInvalidasException si no es válido
	 */
	LoginDTO refrescar(String refreshToken);

	/** Revoca el refresh token. No falla si ya no era válido. */
	void cerrarSesion(String refreshToken);

}
