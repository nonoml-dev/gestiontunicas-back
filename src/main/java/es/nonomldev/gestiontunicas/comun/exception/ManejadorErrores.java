package es.nonomldev.gestiontunicas.comun.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Traduce las excepciones de negocio a respuestas {@link ProblemDetail}.
 * El front muestra el campo {@code detail} tal cual, así que los mensajes van en español y claros.
 */
@RestControllerAdvice
public class ManejadorErrores {

	@ExceptionHandler(RecursoNoEncontradoException.class)
	public ProblemDetail recursoNoEncontrado(RecursoNoEncontradoException ex) {
		return problema(HttpStatus.NOT_FOUND, "No encontrado", ex.getMessage());
	}

	@ExceptionHandler(ParametroNoValidoException.class)
	public ProblemDetail parametroNoValido(ParametroNoValidoException ex) {
		return problema(HttpStatus.BAD_REQUEST, "Parámetro no válido", ex.getMessage());
	}

	@ExceptionHandler(ReglaNegocioException.class)
	public ProblemDetail reglaNegocio(ReglaNegocioException ex) {
		return problema(HttpStatus.CONFLICT, "Operación no permitida", ex.getMessage());
	}

	@ExceptionHandler(CredencialesInvalidasException.class)
	public ProblemDetail credencialesInvalidas(CredencialesInvalidasException ex) {
		return problema(HttpStatus.UNAUTHORIZED, "Sesión rechazada", ex.getMessage());
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ProblemDetail ficheroDemasiadoGrande(MaxUploadSizeExceededException ex) {
		return problema(HttpStatus.CONTENT_TOO_LARGE, "Fichero demasiado grande",
				"El fichero es demasiado grande (máximo 5 MB)");
	}

	private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
		problema.setTitle(titulo);
		return problema;
	}

}
