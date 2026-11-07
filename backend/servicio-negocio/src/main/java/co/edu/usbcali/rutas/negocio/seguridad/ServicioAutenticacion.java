package co.edu.usbcali.rutas.negocio.seguridad;

import co.edu.usbcali.rutas.negocio.dominio.Administrador;
import co.edu.usbcali.rutas.negocio.repositorio.AdministradorRepositorio;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Ingreso del administrador: verifica la contraseña y emite el JWT. */
@Service
public class ServicioAutenticacion {

	/** Token emitido y su vencimiento. */
	public record Token(String valor, Instant expiraEn) {
	}

	private final AdministradorRepositorio administradores;

	private final PasswordEncoder codificador;

	private final JwtEncoder codificadorJwt;

	private final RegistroIntentos intentos;

	private final Clock reloj;

	private final Duration expiracion;

	/** Hash de relleno: si el correo no existe se compara igual, así el tiempo no lo delata. */
	private final String hashDeRelleno;

	public ServicioAutenticacion(AdministradorRepositorio administradores, PasswordEncoder codificador,
			JwtEncoder codificadorJwt, RegistroIntentos intentos, Clock reloj,
			@Value("${rutas.jwt.expiracion}") Duration expiracion) {
		this.administradores = administradores;
		this.codificador = codificador;
		this.codificadorJwt = codificadorJwt;
		this.intentos = intentos;
		this.reloj = reloj;
		this.expiracion = expiracion;
		this.hashDeRelleno = codificador.encode("contrasena-de-relleno");
	}

	/**
	 * @throws DemasiadosIntentosException si el correo está bloqueado
	 * @throws CredencialesInvalidasException si el correo o la contraseña no coinciden
	 */
	public Token ingresar(String correo, String contrasena) {
		if (intentos.estaBloqueado(correo)) {
			throw new DemasiadosIntentosException();
		}
		Optional<Administrador> administrador = administradores.findByCorreo(correo.trim().toLowerCase(Locale.ROOT));
		String hash = administrador.map(Administrador::getContrasenaCifrada).orElse(hashDeRelleno);
		boolean coincide = codificador.matches(contrasena, hash);
		if (administrador.isEmpty() || !coincide) {
			intentos.registrarFallo(correo);
			throw new CredencialesInvalidasException();
		}
		intentos.registrarExito(correo);
		return emitir(administrador.get().getCorreo());
	}

	private Token emitir(String correo) {
		Instant ahora = reloj.instant();
		Instant vence = ahora.plus(expiracion);
		JwtClaimsSet reclamos = JwtClaimsSet.builder()
			.issuer(ConfiguracionJwt.EMISOR)
			.subject(correo)
			.issuedAt(ahora)
			.expiresAt(vence)
			.build();
		JwsHeader encabezado = JwsHeader.with(MacAlgorithm.HS256).build();
		String valor = codificadorJwt.encode(JwtEncoderParameters.from(encabezado, reclamos)).getTokenValue();
		return new Token(valor, vence);
	}

}
