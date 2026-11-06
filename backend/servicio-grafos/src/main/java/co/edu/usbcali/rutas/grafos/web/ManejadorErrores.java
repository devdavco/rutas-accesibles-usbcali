package co.edu.usbcali.rutas.grafos.web;

import co.edu.usbcali.rutas.grafos.servicio.SolicitudRutaInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Convierte los errores en respuestas ProblemDetail (RFC 9457) con mensajes en español. */
@RestControllerAdvice
public class ManejadorErrores {

	private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

	@ExceptionHandler(SolicitudRutaInvalidaException.class)
	ProblemDetail solicitudInvalida(SolicitudRutaInvalidaException e) {
		return problema(HttpStatus.BAD_REQUEST, "Solicitud inválida", e.getMessage());
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	ProblemDetail parametroFaltante(MissingServletRequestParameterException e) {
		return problema(HttpStatus.BAD_REQUEST, "Solicitud inválida",
				"Falta el parámetro obligatorio '%s'.".formatted(e.getParameterName()));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ProblemDetail parametroInvalido(MethodArgumentTypeMismatchException e) {
		Class<?> tipo = e.getRequiredType();
		String esperado = (tipo == boolean.class || tipo == Boolean.class) ? "true o false" : "un número entero";
		return problema(HttpStatus.BAD_REQUEST, "Solicitud inválida",
				"El parámetro '%s' debe ser %s; se recibió '%s'.".formatted(e.getName(), esperado, e.getValue()));
	}

	@ExceptionHandler(RestClientException.class)
	ProblemDetail negocioNoDisponible(RestClientException e) {
		LOG.error("No se pudo obtener el grafo desde servicio-negocio", e);
		return problema(HttpStatus.SERVICE_UNAVAILABLE, "Servicio no disponible",
				"No se pudo obtener el grafo del campus. Intente de nuevo en unos minutos.");
	}

	private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
		problema.setTitle(titulo);
		return problema;
	}

}
