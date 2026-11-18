package es.nonomldev.gestiontunicas.comun.exception;

/** Un parámetro de la petición tiene un valor que no se admite (se traduce a 400). */
public class ParametroNoValidoException extends RuntimeException {

	public ParametroNoValidoException(String mensaje) {
		super(mensaje);
	}

}
