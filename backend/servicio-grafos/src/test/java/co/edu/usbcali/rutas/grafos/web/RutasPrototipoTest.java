package co.edu.usbcali.rutas.grafos.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.usbcali.rutas.grafos.soporte.PruebaConNegocioSimulado;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Prueba de extremo a extremo descrita en la tesis: para 15 pares origen-destino, servicio-grafos
 * debe dar la misma secuencia de puntos que el Dijkstra del prototipo y una distancia que difiera
 * menos de 1 m. Los resultados esperados los genera backend/herramientas/generar-referencia-rutas.mjs.
 */
class RutasPrototipoTest extends PruebaConNegocioSimulado {

	private static final double TOLERANCIA_METROS = 1.0;

	record RutaEsperada(long origen, long destino, String descripcion, List<Long> ruta, double metros) {

		@Override
		public String toString() {
			return descripcion;
		}

	}

	static Stream<RutaEsperada> rutasDelPrototipo() {
		try (InputStream entrada = RutasPrototipoTest.class.getResourceAsStream("/prototipo/rutas-esperadas.json")) {
			JsonNode raiz = JsonMapper.builder().build().readTree(entrada);
			List<RutaEsperada> rutas = new ArrayList<>();
			for (JsonNode r : raiz.get("rutas")) {
				List<Long> ruta = new ArrayList<>();
				r.get("ruta").forEach(id -> ruta.add(id.asLong()));
				rutas.add(new RutaEsperada(r.get("origen").asLong(), r.get("destino").asLong(),
						r.get("origen").asLong() + " → " + r.get("destino").asLong() + " ("
								+ r.get("destinoNombre").asString() + ")",
						ruta, r.get("metros").asDouble()));
			}
			return rutas.stream();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("rutasDelPrototipo")
	void coincideConElPrototipo(RutaEsperada esperada) throws Exception {
		String cuerpo = mockMvc
			.perform(get("/api/rutas").param("origen", String.valueOf(esperada.origen()))
				.param("destino", String.valueOf(esperada.destino())))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();

		List<Number> ids = JsonPath.read(cuerpo, "$.puntos[*].id");
		List<Long> ruta = ids.stream().map(Number::longValue).toList();
		double distancia = JsonPath.<Number>read(cuerpo, "$.distanciaM").doubleValue();

		System.out.printf(Locale.ROOT, "PAR %-45s prototipo %7.2f m / %2d puntos | servicio %7.2f m / %2d puntos | %s%n",
				esperada.descripcion(), esperada.metros(), esperada.ruta().size(), distancia, ruta.size(),
				ruta.equals(esperada.ruta()) ? "misma secuencia" : "SECUENCIA DISTINTA");

		assertThat(JsonPath.<Boolean>read(cuerpo, "$.encontrada")).isTrue();
		assertThat(ruta).as("secuencia de puntos").isEqualTo(esperada.ruta());
		assertThat(Math.abs(distancia - esperada.metros())).as("diferencia de distancia")
			.isLessThan(TOLERANCIA_METROS);
	}

}
