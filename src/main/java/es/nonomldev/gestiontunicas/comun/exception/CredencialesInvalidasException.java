package es.nonomldev.gestiontunicas.comun.exception;

/** Inicio de sesión rechazado (usuario o contraseña incorrectos, usuario inactivo o bloqueado). */
public class CredencialesInvalidasException extends RuntimeException {

	public CredencialesInvalidasException(String mensaje) {
		super(mensaje);
	}

}
