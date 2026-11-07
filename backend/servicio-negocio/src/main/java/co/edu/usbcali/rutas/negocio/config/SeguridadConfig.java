package co.edu.usbcali.rutas.negocio.config;

import co.edu.usbcali.rutas.negocio.seguridad.PuntoEntradaNoAutenticado;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad de servicio-negocio.
 * - Públicos: salud, lectura interna del grafo e ingreso del administrador.
 * - /api/admin/**: exige un JWT válido en el encabezado Authorization (OAuth2 Resource Server).
 * Sin sesión ni CSRF: el token viaja en un encabezado, no en una cookie, así que el navegador
 * no lo envía solo y no hay falsificación de peticiones entre sitios que proteger.
 */
@Configuration
public class SeguridadConfig {

	@Bean
	SecurityFilterChain cadenaDeFiltros(HttpSecurity http, PuntoEntradaNoAutenticado noAutenticado) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(peticiones -> peticiones
				.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
				// /interno/** lo consume servicio-grafos directamente (no pasa por el gateway).
				// En producción el puerto de este servicio no se publica fuera de la red de Docker,
				// así que estas rutas solo son accesibles para los otros contenedores.
				.requestMatchers("/interno/**").permitAll()
				.requestMatchers(HttpMethod.POST, "/api/admin/autenticacion/ingresar").permitAll()
				.requestMatchers("/api/admin/**").authenticated()
				.anyRequest().denyAll())
			.oauth2ResourceServer(servidor -> servidor
				.jwt(jwt -> { })
				.authenticationEntryPoint(noAutenticado))
			.exceptionHandling(errores -> errores.authenticationEntryPoint(noAutenticado));
		return http.build();
	}

}
