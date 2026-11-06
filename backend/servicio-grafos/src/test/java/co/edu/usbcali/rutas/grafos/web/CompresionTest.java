package co.edu.usbcali.rutas.grafos.web;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usbcali.rutas.grafos.soporte.GrafoPrototipo;
import co.edu.usbcali.rutas.grafos.soporte.ServicioNegocioSimulado;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Con el servidor real (Tomcat), las respuestas JSON grandes se comprimen con gzip. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CompresionTest {

	private static final ServicioNegocioSimulado NEGOCIO = new ServicioNegocioSimulado();

	static {
		NEGOCIO.responder(GrafoPrototipo.cargar().json());
	}

	@LocalServerPort
	private int puerto;

	@DynamicPropertySource
	static void urlNegocio(DynamicPropertyRegistry registro) {
		registro.add("rutas.negocio.url", NEGOCIO::url);
	}

	@Test
	void laRutaConPasosSeComprimeConGzip() throws Exception {
		// El HttpClient del JDK no descomprime: se ve la respuesta tal como sale del servidor
		HttpRequest peticion = HttpRequest
			.newBuilder(URI.create("http://localhost:" + puerto + "/api/rutas?origen=1&destino=2&pasos=true"))
			.header("Accept-Encoding", "gzip")
			.build();

		HttpResponse<byte[]> respuesta = HttpClient.newHttpClient()
			.send(peticion, HttpResponse.BodyHandlers.ofByteArray());

		assertThat(respuesta.statusCode()).isEqualTo(200);
		assertThat(respuesta.headers().firstValue("Content-Encoding")).hasValue("gzip");
	}

}
