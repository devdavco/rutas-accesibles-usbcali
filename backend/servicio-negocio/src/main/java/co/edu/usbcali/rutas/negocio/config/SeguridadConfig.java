package co.edu.usbcali.rutas.negocio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración temporal de seguridad.
 * Deja públicos el endpoint de salud y la lectura interna del grafo; el resto de reglas se
 * define en la etapa de seguridad.
 */
@Configuration
public class SeguridadConfig {

	@Bean
	SecurityFilterChain cadenaDeFiltros(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(peticiones -> peticiones
				.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
				// /interno/** lo consume servicio-grafos directamente (no pasa por el gateway).
				// En producción el puerto de este servicio no se publica fuera de la red de Docker,
				// así que estas rutas solo son accesibles para los otros contenedores.
				.requestMatchers("/interno/**").permitAll()
				.anyRequest().authenticated())
			.httpBasic(Customizer.withDefaults());
		return http.build();
	}

}
