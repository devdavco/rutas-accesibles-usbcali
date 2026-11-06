package co.edu.usbcali.rutas.negocio.grafo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Tramo tal como aparece en el prototipo de validación (js/datos.js).
 * Se lee de tramos-prototipo.csv, que genera backend/herramientas/generar-carga-grafo.mjs.
 */
record TramoPrototipo(long desde, long hasta, double metros, String condicion) {

	/** Clave del par sin importar el orden, porque el grafo es no dirigido. */
	String clavePar() {
		return Math.min(desde, hasta) + "-" + Math.max(desde, hasta);
	}

	static List<TramoPrototipo> leerTodos() {
		try (InputStream entrada = TramoPrototipo.class.getResourceAsStream("/prototipo/tramos-prototipo.csv")) {
			String contenido = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
			return contenido.lines()
				.skip(1) // encabezado
				.filter(linea -> !linea.isBlank())
				.map(linea -> linea.split(","))
				.map(c -> new TramoPrototipo(Long.parseLong(c[0]), Long.parseLong(c[1]), Double.parseDouble(c[2]), c[3]))
				.toList();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
