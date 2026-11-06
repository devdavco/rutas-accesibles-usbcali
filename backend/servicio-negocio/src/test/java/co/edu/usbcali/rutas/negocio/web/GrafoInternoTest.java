package co.edu.usbcali.rutas.negocio.web;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.usbcali.rutas.negocio.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** Verifica la lectura interna del grafo que consume servicio-grafos. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class GrafoInternoTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void devuelveElGrafoCompletoSinCredenciales() throws Exception {
		mockMvc.perform(get("/interno/grafo"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.puntos", hasSize(94)))
			.andExpect(jsonPath("$.tramos", hasSize(107)))
			// Entrada Principal, con sus coordenadas del prototipo
			.andExpect(jsonPath("$.puntos[0].id").value(1))
			.andExpect(jsonPath("$.puntos[0].nombre").value("Entrada Principal"))
			.andExpect(jsonPath("$.puntos[0].tipo").value("ENTRADA_CAMPUS"))
			.andExpect(jsonPath("$.puntos[0].seleccionable").value(true))
			.andExpect(jsonPath("$.puntos[0].lat").value(closeTo(3.34252136, 1e-8)))
			.andExpect(jsonPath("$.puntos[0].lng").value(closeTo(-76.54407333, 1e-8)))
			// Primer tramo de V3: 4 → 2, plano, ≈ 23,6 m
			.andExpect(jsonPath("$.tramos[0].puntoInicialId").value(4))
			.andExpect(jsonPath("$.tramos[0].puntoFinalId").value(2))
			.andExpect(jsonPath("$.tramos[0].condicion").value("PLANO"))
			.andExpect(jsonPath("$.tramos[0].longitudM").value(closeTo(23.6, 1.0)));
	}

	@Test
	void elRestoDeRutasSigueProtegido() throws Exception {
		mockMvc.perform(get("/api/admin/puntos")).andExpect(status().isUnauthorized());
	}

}
