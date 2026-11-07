package co.edu.usbcali.rutas.negocio.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.usbcali.rutas.negocio.TestcontainersConfiguration;
import co.edu.usbcali.rutas.negocio.seguridad.RegistroIntentos;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base de las pruebas del API de administración. El administrador de pruebas
 * (src/test/resources/config/application.yml) se crea al arrancar el contexto.
 */
@Import({ TestcontainersConfiguration.class, PruebaAdmin.ConfiguracionReloj.class })
@SpringBootTest
@AutoConfigureMockMvc
public abstract class PruebaAdmin {

	protected static final String CORREO = "admin@usbcali.edu.co";

	protected static final String CONTRASENA = "clave-de-pruebas";

	@TestConfiguration(proxyBeanMethods = false)
	static class ConfiguracionReloj {

		@Bean
		@Primary
		RelojAjustable relojAjustable() {
			return new RelojAjustable();
		}

	}

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected RelojAjustable reloj;

	@Autowired
	private RegistroIntentos intentos;

	@BeforeEach
	void reiniciarEstado() {
		reloj.reiniciar();
		intentos.reiniciar();
	}

	protected static String cuerpoIngreso(String correo, String contrasena) {
		return """
				{"correo": "%s", "contrasena": "%s"}""".formatted(correo, contrasena);
	}

	/** Ingresa como administrador y devuelve el encabezado "Bearer …". */
	protected String token() throws Exception {
		String respuesta = mockMvc
			.perform(post("/api/admin/autenticacion/ingresar").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpoIngreso(CORREO, CONTRASENA)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return "Bearer " + JsonPath.read(respuesta, "$.token");
	}

}
