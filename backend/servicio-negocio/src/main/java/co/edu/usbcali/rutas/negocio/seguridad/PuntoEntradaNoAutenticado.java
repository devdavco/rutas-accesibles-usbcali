package co.edu.usbcali.rutas.negocio.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Respuesta 401 para /api/admin/** sin token, con token inválido o vencido: un ProblemDetail
 * en español. No dice cuál fue la causa, para no dar pistas.
 */
@Component
public class PuntoEntradaNoAutenticado implements AuthenticationEntryPoint {

	private static final String CUERPO = """
			{"type":"about:blank","title":"No autenticado","status":401,\
			"detail":"Se requiere iniciar sesión."}""";

	@Override
	public void commence(HttpServletRequest peticion, HttpServletResponse respuesta, AuthenticationException e)
			throws IOException {
		respuesta.setStatus(HttpStatus.UNAUTHORIZED.value());
		respuesta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
		respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
		respuesta.getWriter().write(CUERPO);
	}

}
