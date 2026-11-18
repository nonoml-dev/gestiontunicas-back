package es.nonomldev.gestiontunicas.comun.exception;

/** No existe el recurso pedido (se traducirá a 404). */
public class RecursoNoEncontradoException extends RuntimeException {

	public RecursoNoEncontradoException(String recurso, Object id) {
		super("No existe %s con id %s".formatted(recurso, id));
	}

	public RecursoNoEncontradoException(String mensaje) {
		super(mensaje);
	}

}
