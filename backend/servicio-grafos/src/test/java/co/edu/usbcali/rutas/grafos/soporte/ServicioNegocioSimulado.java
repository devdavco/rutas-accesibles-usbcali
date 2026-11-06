package co.edu.usbcali.rutas.grafos.soporte;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Servidor HTTP mínimo que hace de servicio-negocio en las pruebas: responde GET /interno/grafo
 * con el JSON que se le indique. Así se prueba de verdad el RestClient de servicio-grafos.
 */
public final class ServicioNegocioSimulado {

	private final HttpServer servidor;

	private volatile int estado = 200;

	private volatile String cuerpo = "{}";

	private volatile int llamadas;

	public ServicioNegocioSimulado() {
		try {
			servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		servidor.createContext("/interno/grafo", intercambio -> {
			llamadas++;
			byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
			intercambio.getResponseHeaders().add("Content-Type", "application/json");
			intercambio.sendResponseHeaders(estado, bytes.length);
			try (OutputStream salida = intercambio.getResponseBody()) {
				salida.write(bytes);
			}
		});
		servidor.start();
	}

	public String url() {
		return "http://127.0.0.1:" + servidor.getAddress().getPort();
	}

	/** Responde 200 con este grafo. */
	public void responder(String json) {
		this.estado = 200;
		this.cuerpo = json;
	}

	/** Responde con un error HTTP. */
	public void fallar(int estado) {
		this.estado = estado;
		this.cuerpo = "{\"error\":\"simulado\"}";
	}

	public int llamadas() {
		return llamadas;
	}

}
