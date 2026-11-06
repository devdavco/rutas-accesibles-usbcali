package co.edu.usbcali.rutas.grafos.cliente;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente REST hacia servicio-negocio. Llama directamente al servicio (no pasa por el gateway).
 */
@Component
public class ClienteNegocio {

	private final RestClient restClient;

	public ClienteNegocio(RestClient.Builder constructor, @Value("${rutas.negocio.url}") String urlNegocio) {
		this.restClient = constructor.baseUrl(urlNegocio).build();
	}

	/**
	 * Descarga el grafo completo. Se llama en cada cálculo de ruta (RF10), así cualquier cambio
	 * hecho por el administrador se refleja de inmediato.
	 *
	 * @throws RestClientException si servicio-negocio no responde o responde con error
	 */
	public GrafoNegocio obtenerGrafo() {
		GrafoNegocio grafo = restClient.get().uri("/interno/grafo").retrieve().body(GrafoNegocio.class);
		if (grafo == null) {
			throw new RestClientException("servicio-negocio respondió sin contenido");
		}
		return grafo;
	}

}
