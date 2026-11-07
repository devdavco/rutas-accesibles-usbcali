package co.edu.usbcali.rutas.grafos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.usbcali.rutas.grafos.dominio.Condicion;
import co.edu.usbcali.rutas.grafos.soporte.PruebaConNegocioSimulado;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Contrato HTTP de /api/rutas y /api/rutas/puntos, con el grafo del prototipo. */
class RutasControllerTest extends PruebaConNegocioSimulado {

	@Test
	void listaSoloLosPuntosSeleccionables() throws Exception {
		mockMvc.perform(get("/api/rutas/puntos"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(16)))
			.andExpect(jsonPath("$[*].tipo", everyItem(org.hamcrest.Matchers.not("CRUCE"))))
			// Las entradas del campus (sin edificio) van primero
			.andExpect(jsonPath("$[0].nombre").value("Entrada de Estudiantes (Parqueadero)"))
			.andExpect(jsonPath("$[0].edificio", nullValue()))
			.andExpect(jsonPath("$[1].nombre").value("Entrada Principal"))
			.andExpect(jsonPath("$[2].edificio").value("Biblioteca"));
	}

	@Test
	void devuelveElGrafoCompletoParaElMapa() throws Exception {
		mockMvc.perform(get("/api/rutas/grafo"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.puntos", hasSize(94)))
			// Incluye el tramo con escaleras: el mapa puede mostrarlo como excluido
			.andExpect(jsonPath("$.tramos", hasSize(107)))
			.andExpect(jsonPath("$.tramos[?(@.condicion == 'ESCALERAS')]", hasSize(1)))
			.andExpect(jsonPath("$.puntos[0].nombre").value("Entrada Principal"))
			.andExpect(jsonPath("$.puntos[0].lat").value(closeTo(3.34252136, 1e-8)))
			.andExpect(jsonPath("$.tramos[0].puntoInicialId").value(4))
			// Longitud y peso calculados en el backend: el editor no repite la regla de pesos
			.andExpect(jsonPath("$.tramos[0].longitudM").value(23.6))
			.andExpect(jsonPath("$.tramos[0].pesoCalculado").value(23.6))
			// Escaleras: no entran al grafo, su peso es null
			.andExpect(jsonPath("$.tramos[?(@.condicion == 'ESCALERAS')].pesoCalculado").value(org.hamcrest.Matchers.contains(org.hamcrest.Matchers.nullValue())));
	}

	@Test
	void elPesoDeLaPendienteFuerteEsElDoble() throws Exception {
		NEGOCIO.responder(PROTOTIPO.conCondicion(4, 2, Condicion.PENDIENTE_FUERTE).json());
		mockMvc.perform(get("/api/rutas/grafo"))
			.andExpect(jsonPath("$.tramos[0].condicion").value("PENDIENTE_FUERTE"))
			.andExpect(jsonPath("$.tramos[0].pesoCalculado").value(47.2));
	}

	@Test
	void rutaSinPasos() throws Exception {
		String cuerpo = mockMvc.perform(get("/api/rutas").param("origen", "1").param("destino", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.encontrada").value(true))
			.andExpect(jsonPath("$.origen.nombre").value("Entrada Principal"))
			.andExpect(jsonPath("$.destino.nombre").value("Edificio El Cedro (Entrada Lateral)"))
			.andExpect(jsonPath("$.mensaje", containsString("Ruta accesible encontrada")))
			.andExpect(jsonPath("$.pasos").doesNotExist())
			.andExpect(jsonPath("$.geojson.type").value("FeatureCollection"))
			.andExpect(jsonPath("$.geojson.features", hasSize(3)))
			.andExpect(jsonPath("$.geojson.features[0].geometry.type").value("LineString"))
			.andExpect(jsonPath("$.geojson.features[1].properties.tipo").value("origen"))
			.andExpect(jsonPath("$.geojson.features[2].properties.tipo").value("destino"))
			// GeoJSON usa [longitud, latitud]
			.andExpect(jsonPath("$.geojson.features[1].geometry.coordinates[0]").value(closeTo(-76.54407333, 1e-8)))
			.andExpect(jsonPath("$.geojson.features[1].geometry.coordinates[1]").value(closeTo(3.34252136, 1e-8)))
			.andReturn().getResponse().getContentAsString();

		List<?> puntos = JsonPath.read(cuerpo, "$.puntos");
		List<?> coordenadas = JsonPath.read(cuerpo, "$.geojson.features[0].geometry.coordinates");
		assertThat(coordenadas).hasSameSizeAs(puntos);

		// Los segmentos cubren toda la ruta, sin dos seguidos con la misma condición
		List<String> condiciones = JsonPath.read(cuerpo, "$.segmentos[*].condicion");
		List<Number> metros = JsonPath.read(cuerpo, "$.segmentos[*].metros");
		for (int i = 1; i < condiciones.size(); i++) {
			assertThat(condiciones.get(i)).isNotEqualTo(condiciones.get(i - 1));
		}
		double suma = metros.stream().mapToDouble(Number::doubleValue).sum();
		assertThat(suma).isCloseTo(JsonPath.<Number>read(cuerpo, "$.distanciaM").doubleValue(),
				org.assertj.core.data.Offset.offset(0.05));
		assertThat(JsonPath.<Number>read(cuerpo, "$.segmentos[0].desdeId").longValue()).isEqualTo(1);
		assertThat(JsonPath.<Number>read(cuerpo, "$.segmentos[-1].hastaId").longValue()).isEqualTo(2);
	}

	@Test
	void rutaConPasos() throws Exception {
		mockMvc.perform(get("/api/rutas").param("origen", "1").param("destino", "2").param("pasos", "true"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.pasos[0].tipo").value("INICIO"))
			.andExpect(jsonPath("$.pasos[0].numero").value(1))
			.andExpect(jsonPath("$.pasos[-1].tipo").value("FIN"))
			.andExpect(jsonPath("$.pasos[-1].mensaje", containsString("Edificio El Cedro (Entrada Lateral)")));
	}

	@Test
	void origenIgualADestino() throws Exception {
		mockMvc.perform(get("/api/rutas").param("origen", "11").param("destino", "11"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.encontrada").value(true))
			.andExpect(jsonPath("$.distanciaM").value(0.0))
			.andExpect(jsonPath("$.puntos", hasSize(1)))
			.andExpect(jsonPath("$.segmentos", hasSize(0)))
			// Sin LineString: solo los puntos de origen y destino
			.andExpect(jsonPath("$.geojson.features", hasSize(2)))
			.andExpect(jsonPath("$.mensaje").value("El origen y el destino son el mismo punto."));
	}

	@Test
	void sinRutaAccesibleRespondeOkConEncontradaFalse() throws Exception {
		// Si el único tramo que llega a Higuerones 1 (41-42) tuviera escaleras, no habría ruta
		NEGOCIO.responder(PROTOTIPO.conCondicion(41, 42, Condicion.ESCALERAS).json());

		mockMvc.perform(get("/api/rutas").param("origen", "1").param("destino", "42").param("pasos", "true"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.encontrada").value(false))
			.andExpect(jsonPath("$.mensaje", containsString("No existe una ruta accesible")))
			.andExpect(jsonPath("$.distanciaM").doesNotExist())
			.andExpect(jsonPath("$.geojson").doesNotExist())
			.andExpect(jsonPath("$.puntos", hasSize(0)))
			.andExpect(jsonPath("$.pasos[-1].tipo").value("SIN_RUTA"));
	}

	@Test
	void elGrafoSeCargaEnCadaCalculo() throws Exception {
		int antes = NEGOCIO.llamadas();
		mockMvc.perform(get("/api/rutas").param("origen", "1").param("destino", "2"));
		mockMvc.perform(get("/api/rutas").param("origen", "1").param("destino", "2"));
		assertThat(NEGOCIO.llamadas() - antes).isEqualTo(2);
	}

	@Test
	void puntoInexistenteEs400() throws Exception {
		mockMvc.perform(get("/api/rutas").param("origen", "1").param("destino", "999"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Solicitud inválida"))
			.andExpect(jsonPath("$.detail").value("El punto de destino 999 no existe."));
	}

	@Test
	void puntoNoSeleccionableEs400() throws Exception {
		mockMvc.perform(get("/api/rutas").param("origen", "4").param("destino", "2"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail",
					is("El punto de origen 4 (Giro El Cedro) no se puede elegir como origen o destino.")));
	}

	@Test
	void destinoFueraDelAlcanceEs400() throws Exception {
		// Capilla: se carga para la conectividad, pero está fuera del alcance de la tesis
		mockMvc.perform(get("/api/rutas").param("origen", "1").param("destino", "27"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void parametroFaltanteEs400() throws Exception {
		mockMvc.perform(get("/api/rutas").param("origen", "1"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Falta el parámetro obligatorio 'destino'."));
	}

	@Test
	void parametroNoNumericoEs400() throws Exception {
		mockMvc.perform(get("/api/rutas").param("origen", "abc").param("destino", "2"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("El parámetro 'origen' debe ser un número entero; se recibió 'abc'."));
	}

	@Test
	void servicioNegocioCaidoEs503() throws Exception {
		NEGOCIO.fallar(500);

		mockMvc.perform(get("/api/rutas").param("origen", "1").param("destino", "2"))
			.andExpect(status().isServiceUnavailable())
			.andExpect(jsonPath("$.title").value("Servicio no disponible"));
	}

}
