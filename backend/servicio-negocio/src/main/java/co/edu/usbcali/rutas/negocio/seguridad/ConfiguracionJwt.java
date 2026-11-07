package co.edu.usbcali.rutas.negocio.seguridad;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Emisión y verificación de JWT con el soporte nativo de Spring Security (Nimbus), firma HS256.
 * La clave sale de JWT_SECRETO y no tiene valor por defecto.
 */
@Configuration
public class ConfiguracionJwt {

	/** Quien emite los tokens; el decodificador rechaza tokens de otro emisor. */
	public static final String EMISOR = "servicio-negocio";

	/** HS256 exige una clave de al menos 256 bits. */
	private static final int BYTES_MINIMOS = 32;

	@Bean
	SecretKey claveJwt(@Value("${rutas.jwt.secreto}") String secreto) {
		byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < BYTES_MINIMOS) {
			throw new IllegalStateException("JWT_SECRETO debe tener al menos " + BYTES_MINIMOS
					+ " bytes (256 bits). Puede generarlo con: openssl rand -base64 48");
		}
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

	@Bean
	JwtEncoder codificadorJwt(SecretKey clave) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
	}

	@Bean
	JwtDecoder decodificadorJwt(SecretKey clave, Clock reloj) {
		NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
		JwtTimestampValidator vigencia = new JwtTimestampValidator();
		vigencia.setClock(reloj);
		decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(vigencia, new JwtIssuerValidator(EMISOR)));
		return decodificador;
	}

	@Bean
	PasswordEncoder codificadorContrasenas() {
		return new BCryptPasswordEncoder();
	}

	/** Reloj del sistema; las pruebas lo reemplazan para simular el paso del tiempo. */
	@Bean
	Clock reloj() {
		return Clock.systemUTC();
	}

}
