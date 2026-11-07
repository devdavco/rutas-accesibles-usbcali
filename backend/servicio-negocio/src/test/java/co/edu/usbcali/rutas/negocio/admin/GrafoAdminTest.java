package co.edu.usbcali.rutas.negocio.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** CRUD de puntos y tramos con token. Cada prueba se revierte al terminar. */
@Transactional
class GrafoAdminTest extends PruebaAdmin {

	@Autowired
	private JdbcTemplate jdbc;

	private String autorizacion;

	@BeforeEach
	void ingresar() throws Exception {
		autorizacion = token();
	}

	private ResultActions enviar(MockHttpServletRequestBuilder peticion, String cuerpo) throws Exception {
		peticion.header(HttpHeaders.AUTHORIZATION, autorizacion);
		if (cuerpo != null) {
			peticion.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
		}
		return mockMvc.perform(peticion);
	}

	private static String punto(String nombre, String tipo, String edificio, boolean seleccionable, double lat,
			double lng) {
		return """
				{"nombre": "%s", "tipo": "%s", "edificio": %s, "seleccionable": %b, "lat": %s, "lng": %s}"""
			.formatted(nombre, tipo, edificio == null ? "null" : "\"" + edificio + "\"", seleccionable, lat, lng);
	}

	private long crearCruce(String nombre, double lat, double lng) throws Exception {
		String respuesta = enviar(post("/api/admin/puntos"), punto(nombre, "CRUCE", null, false, lat, lng))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
	}

	// --- Puntos ----------------------------------------------------------------------------

	@Test
	void listaTodosLosPuntosIncluidosLosCruces() throws Exception {
		enviar(get("/api/admin/puntos"), null)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(94)))
			.andExpect(jsonPath("$[3].tipo").value("CRUCE"));
	}

	@Test
	void creaEditaYEliminaUnPunto() throws Exception {
		long id = crearCruce("Cruce de prueba", 3.3440, -76.5430);

		enviar(put("/api/admin/puntos/" + id),
				punto("Kiosco de prueba", "ENTRADA_EDIFICIO", "Kiosco", true, 3.3441, -76.5431))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nombre").value("Kiosco de prueba"))
			.andExpect(jsonPath("$.edificio").value("Kiosco"))
			.andExpect(jsonPath("$.lat").value(closeTo(3.3441, 1e-9)));

		enviar(delete("/api/admin/puntos/" + id), null).andExpect(status().isNoContent());
		enviar(put("/api/admin/puntos/" + id), punto("X", "CRUCE", null, false, 3.3, -76.5))
			.andExpect(status().isNotFound());
	}

	@Test
	void nombreRepetidoEs409() throws Exception {
		enviar(post("/api/admin/puntos"), punto("Biblioteca", "ENTRADA_EDIFICIO", "Biblioteca", true, 3.3, -76.5))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Ya existe un punto llamado «Biblioteca»."));
	}

	@Test
	void unCruceSeleccionableEs400() throws Exception {
		enviar(post("/api/admin/puntos"), punto("Cruce nuevo", "CRUCE", null, true, 3.3, -76.5))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Un cruce no se puede elegir como origen o destino."));
	}

	@Test
	void unaEntradaDeEdificioSinEdificioEs400() throws Exception {
		enviar(post("/api/admin/puntos"), punto("Entrada nueva", "ENTRADA_EDIFICIO", null, true, 3.3, -76.5))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Una entrada de edificio debe indicar el edificio."));
	}

	@Test
	void datosInvalidosSon400ConMensajesEnEspanol() throws Exception {
		enviar(post("/api/admin/puntos"), punto("", "CRUCE", null, false, 95, -76.5))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail", containsString("El nombre es obligatorio.")))
			.andExpect(jsonPath("$.detail", containsString("La latitud debe estar entre -90 y 90.")));
	}

	@Test
	void eliminarUnPuntoConTramosEs409() throws Exception {
		enviar(delete("/api/admin/puntos/1"), null)
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail", containsString("elimínelos primero")));
	}

	@Test
	void moverUnPuntoRecalculaSusTramos() throws Exception {
		double antes = jdbc.queryForObject(
				"SELECT longitud_m FROM tramo WHERE punto_inicial_id = 1 AND punto_final_id = 66", Double.class);

		// La Entrada Principal se mueve unos 11 m al sur
		enviar(put("/api/admin/puntos/1"),
				punto("Entrada Principal", "ENTRADA_CAMPUS", null, true, 3.34242136, -76.54407333))
			.andExpect(status().isOk());

		String tramos = enviar(get("/api/admin/tramos"), null).andReturn().getResponse().getContentAsString();
		Number despues = JsonPath.<java.util.List<Number>>read(tramos,
				"$[?(@.puntoInicialId == 1 && @.puntoFinalId == 66)].longitudM").getFirst();
		assertThat(despues.doubleValue()).isNotEqualTo(antes).isGreaterThan(antes);
	}

	// --- Tramos ----------------------------------------------------------------------------

	@Test
	void creaCambiaYEliminaUnTramoConLongitudCalculadaPorPostgis() throws Exception {
		long a = crearCruce("Cruce A", 3.3440, -76.5430);
		long b = crearCruce("Cruce B", 3.3441, -76.5430); // ≈ 11,1 m al norte

		String creado = enviar(post("/api/admin/tramos"),
				"""
						{"puntoInicialId": %d, "puntoFinalId": %d, "condicion": "PLANO"}""".formatted(a, b))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.longitudM").value(closeTo(11.06, 0.05)))
			.andReturn().getResponse().getContentAsString();
		long id = ((Number) JsonPath.read(creado, "$.id")).longValue();

		enviar(put("/api/admin/tramos/" + id), """
				{"condicion": "PENDIENTE_FUERTE"}""")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.condicion").value("PENDIENTE_FUERTE"));

		enviar(delete("/api/admin/tramos/" + id), null).andExpect(status().isNoContent());
		enviar(delete("/api/admin/tramos/" + id), null).andExpect(status().isNotFound());
	}

	@Test
	void tramoDuplicadoEnOrdenInversoEs409() throws Exception {
		// En el prototipo existe el tramo 1 → 66
		enviar(post("/api/admin/tramos"), """
				{"puntoInicialId": 66, "puntoFinalId": 1, "condicion": "RAMPA"}""")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value("Ya existe un tramo entre «Cruce 66» y «Entrada Principal»."));
	}

	@Test
	void tramoDeUnPuntoASiMismoEs400() throws Exception {
		enviar(post("/api/admin/tramos"), """
				{"puntoInicialId": 5, "puntoFinalId": 5, "condicion": "PLANO"}""")
			.andExpect(status().isBadRequest());
	}

	@Test
	void condicionInexistenteEs400() throws Exception {
		enviar(put("/api/admin/tramos/1"), """
				{"condicion": "PENDIENTE"}""")
			.andExpect(status().isBadRequest());
	}

	@Test
	void tramoHaciaUnPuntoInexistenteEs404() throws Exception {
		enviar(post("/api/admin/tramos"), """
				{"puntoInicialId": 1, "puntoFinalId": 9999, "condicion": "PLANO"}""")
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail").value("El punto 9999 no existe."));
	}

}
