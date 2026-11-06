package co.edu.usbcali.rutas.negocio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración temporal de seguridad.
 * Solo deja público el endpoint de salud; el resto de reglas se define en otra etapa.
 */
@Configuration
public class SeguridadConfig {

	@Bean
	SecurityFilterChain cadenaDeFiltros(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(peticiones -> peticiones
				.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
				.anyRequest().authenticated())
			.httpBasic(Customizer.withDefaults());
		return http.build();
	}

}
