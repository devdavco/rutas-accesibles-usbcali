package co.edu.usbcali.rutas.gateway.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS para que el frontend (en desarrollo, http://localhost:4200) pueda llamar al API.
 * Se usa un filtro de servlet porque así cubre las rutas del gateway y responde las
 * peticiones preflight (OPTIONS) sin reenviarlas a los servicios.
 */
@Configuration
public class CorsConfig {

	@Bean
	CorsFilter filtroCors(@Value("${rutas.cors.origenes-permitidos}") List<String> origenesPermitidos) {
		CorsConfiguration configuracion = new CorsConfiguration();
		configuracion.setAllowedOrigins(origenesPermitidos.stream().map(String::trim).toList());
		configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		// Encabezados que envía el frontend: el token del administrador y el tipo del cuerpo
		configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		configuracion.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/api/**", configuracion);
		return new CorsFilter(fuente);
	}

}
