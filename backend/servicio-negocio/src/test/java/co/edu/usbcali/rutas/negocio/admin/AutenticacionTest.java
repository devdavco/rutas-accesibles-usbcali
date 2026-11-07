package co.edu.usbcali.rutas.negocio.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.usbcali.rutas.negocio.seguridad.ConfiguracionJwt;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.ResultActions;

class AutenticacionTest extends PruebaAdmin {

	@Autowired
	private JwtEncoder codificador;

	private ResultActions ingresar(String correo, String contrasena) throws Exception {
		return mockMvc.perform(post("/api/admin/autenticacion/ingresar").contentType(MediaType.APPLICATION_JSON)
			.content(cuerpoIngreso(correo, contrasena)));
	}

	@Test
	void ingresoCorrectoDevuelveTokenQueVenceEnDosHoras() throws Exception {
		Instant antes = reloj.instant();
		String cuerpo = ingresar(CORREO, CONTRASENA)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token", notNullValue()))
			.andReturn().getResponse().getContentAsString();

		Instant expira = Instant.parse(com.jayway.jsonpath.JsonPath.read(cuerpo, "$.expiraEn"));
		assertThat(Duration.between(antes, expira)).isEqualTo(Duration.ofHours(2));
	}

	@Test
	void conTokenSeAccedeAlApiDeAdministracion() throws Exception {
		mockMvc.perform(get("/api/admin/puntos").header(HttpHeaders.AUTHORIZATION, token()))
			.andExpect(status().isOk());
	}

	@Test
	void contrasenaIncorrectaYCorreoInexistenteDanElMismoMensaje() throws Exception {
		ingresar(CORREO, "otra-clave")
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos."));
		ingresar("nadie@usbcali.edu.co", "otra-clave")
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Correo o contraseña incorrectos."));
	}

	@Test
	void camposVaciosSon400() throws Exception {
		ingresar("", "").andExpect(status().isBadRequest());
	}

	@Test
	void cincoFallosSeguidosBloqueanElCorreoCincoMinutos() throws Exception {
		for (int i = 0; i < 5; i++) {
			ingresar(CORREO, "mala-" + i).andExpect(status().isUnauthorized());
		}
		// Bloqueado: ni siquiera la contraseña correcta entra
		ingresar(CORREO, CONTRASENA)
			.andExpect(status().isTooManyRequests())
			.andExpect(jsonPath("$.detail").value("Demasiados intentos. Intente de nuevo en unos minutos."));

		reloj.adelantar(Duration.ofMinutes(4));
		ingresar(CORREO, CONTRASENA).andExpect(status().isTooManyRequests());

		reloj.adelantar(Duration.ofMinutes(1).plusSeconds(1));
		ingresar(CORREO, CONTRASENA).andExpect(status().isOk());
	}

	@Test
	void unIngresoCorrectoReiniciaElConteoDeFallos() throws Exception {
		for (int i = 0; i < 4; i++) {
			ingresar(CORREO, "mala").andExpect(status().isUnauthorized());
		}
		ingresar(CORREO, CONTRASENA).andExpect(status().isOk());
		ingresar(CORREO, "mala").andExpect(status().isUnauthorized());
		ingresar(CORREO, CONTRASENA).andExpect(status().isOk());
	}

	@Test
	void sinToken401() throws Exception {
		mockMvc.perform(get("/api/admin/puntos"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Se requiere iniciar sesión."));
	}

	@Test
	void tokenFirmadoConOtraClave401() throws Exception {
		var ajena = new SecretKeySpec("otra-clave-de-al-menos-32-bytes-para-hs256".getBytes(StandardCharsets.UTF_8),
				"HmacSHA256");
		String token = emitir(new NimbusJwtEncoder(new ImmutableSecret<>(ajena)), reloj.instant().plusSeconds(3600));

		mockMvc.perform(get("/api/admin/puntos").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenMalFormado401() throws Exception {
		mockMvc.perform(get("/api/admin/puntos").header(HttpHeaders.AUTHORIZATION, "Bearer no-es-un-jwt"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenVencido401() throws Exception {
		String token = token();
		mockMvc.perform(get("/api/admin/puntos").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());

		// Pasan las 2 horas de vigencia (y el minuto de tolerancia del validador)
		reloj.adelantar(Duration.ofHours(2).plusMinutes(2));
		mockMvc.perform(get("/api/admin/puntos").header(HttpHeaders.AUTHORIZATION, token))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenVencidoEmitidoConLaClaveCorrecta401() throws Exception {
		String token = emitir(codificador, reloj.instant().minus(Duration.ofHours(1)));
		mockMvc.perform(get("/api/admin/puntos").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void lasRutasPublicasSiguenPublicas() throws Exception {
		mockMvc.perform(get("/interno/grafo")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	private String emitir(JwtEncoder codificador, Instant vence) {
		JwtClaimsSet reclamos = JwtClaimsSet.builder()
			.issuer(ConfiguracionJwt.EMISOR)
			.subject(CORREO)
			.issuedAt(vence.minus(Duration.ofHours(2)))
			.expiresAt(vence)
			.build();
		return codificador.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), reclamos))
			.getTokenValue();
	}

}
