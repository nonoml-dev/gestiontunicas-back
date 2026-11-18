package es.nonomldev.gestiontunicas.comun.exception;

/**
 * Se ha incumplido una regla de negocio (se traducirá a 409/422).
 * El mensaje se muestra tal cual al usuario: tiene que ser claro y en español.
 */
public class ReglaNegocioException extends RuntimeException {

	public ReglaNegocioException(String mensaje) {
		super(mensaje);
	}

}
