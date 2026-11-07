package co.edu.usbcali.rutas.negocio.web.admin;

import co.edu.usbcali.rutas.negocio.seguridad.CredencialesInvalidasException;
import co.edu.usbcali.rutas.negocio.seguridad.DemasiadosIntentosException;
import co.edu.usbcali.rutas.negocio.servicio.ConflictoException;
import co.edu.usbcali.rutas.negocio.servicio.RecursoNoEncontradoException;
import co.edu.usbcali.rutas.negocio.servicio.SolicitudInvalidaException;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores del API de administración como ProblemDetail (RFC 9457) en español. */
@RestControllerAdvice
public class ManejadorErroresAdmin {

	@ExceptionHandler(CredencialesInvalidasException.class)
	ProblemDetail credenciales(CredencialesInvalidasException e) {
		return problema(HttpStatus.UNAUTHORIZED, "No autenticado", e.getMessage());
	}

	@ExceptionHandler(DemasiadosIntentosException.class)
	ProblemDetail demasiadosIntentos(DemasiadosIntentosException e) {
		return problema(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos", e.getMessage());
	}

	@ExceptionHandler(RecursoNoEncontradoException.class)
	ProblemDetail noEncontrado(RecursoNoEncontradoException e) {
		return problema(HttpStatus.NOT_FOUND, "No encontrado", e.getMessage());
	}

	@ExceptionHandler(ConflictoException.class)
	ProblemDetail conflicto(ConflictoException e) {
		return problema(HttpStatus.CONFLICT, "Conflicto", e.getMessage());
	}

	@ExceptionHandler(SolicitudInvalidaException.class)
	ProblemDetail invalida(SolicitudInvalidaException e) {
		return problema(HttpStatus.BAD_REQUEST, "Solicitud inválida", e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail validacion(MethodArgumentNotValidException e) {
		String detalle = e.getBindingResult().getFieldErrors().stream()
			.map(error -> error.getDefaultMessage())
			.distinct()
			.collect(Collectors.joining(" "));
		return problema(HttpStatus.BAD_REQUEST, "Solicitud inválida", detalle);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail ilegible(HttpMessageNotReadableException e) {
		return problema(HttpStatus.BAD_REQUEST, "Solicitud inválida",
				"El cuerpo de la solicitud no es válido (revise el formato y los valores permitidos).");
	}

	/**
	 * Último recurso si dos cambios simultáneos pasan las validaciones del servicio: se traduce la
	 * restricción de la base de datos que falló.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail integridad(DataIntegrityViolationException e) {
		String mensaje = String.valueOf(e.getMostSpecificCause().getMessage());
		if (mensaje.contains("uk_punto_nombre")) {
			return problema(HttpStatus.CONFLICT, "Conflicto", "Ya existe un punto con ese nombre.");
		}
		if (mensaje.contains("ux_tramo_par")) {
			return problema(HttpStatus.CONFLICT, "Conflicto", "Ya existe un tramo entre esos dos puntos.");
		}
		if (mensaje.contains("tramo_punto")) {
			return problema(HttpStatus.CONFLICT, "Conflicto", "El punto tiene tramos; elimínelos primero.");
		}
		return problema(HttpStatus.BAD_REQUEST, "Solicitud inválida", "Los datos no cumplen las reglas del grafo.");
	}

	private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
		problema.setTitle(titulo);
		return problema;
	}

}
