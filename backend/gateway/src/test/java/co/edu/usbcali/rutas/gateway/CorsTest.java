package co.edu.usbcali.rutas.gateway;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/** Preflight CORS: el gateway lo responde sin reenviarlo a los servicios. */
@SpringBootTest
@AutoConfigureMockMvc
class CorsTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void permiteElFrontendEnDesarrollo() throws Exception {
		mockMvc.perform(options("/api/rutas")
				.header(HttpHeaders.ORIGIN, "http://localhost:4200")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
	}

	@Test
	void rechazaOtrosOrigenes() throws Exception {
		mockMvc.perform(options("/api/rutas")
				.header(HttpHeaders.ORIGIN, "http://otro-sitio.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(status().isForbidden())
			.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

}
