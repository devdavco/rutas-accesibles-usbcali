package co.edu.usbcali.rutas.grafos.soporte;

import co.edu.usbcali.rutas.grafos.dominio.CalculadoraPesos;
import co.edu.usbcali.rutas.grafos.dominio.Condicion;
import co.edu.usbcali.rutas.grafos.dominio.Grafo;
import co.edu.usbcali.rutas.grafos.dominio.Punto;
import co.edu.usbcali.rutas.grafos.dominio.TipoPunto;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Grafo del prototipo de validación, leído de los CSV que genera
 * backend/herramientas/generar-carga-grafo.mjs. Las longitudes son los metros del prototipo.
 */
public record GrafoPrototipo(List<Punto> puntos, List<TramoPrototipo> tramos) {

	/** Tramo del CSV; su id es la posición en el archivo (1..n), igual que en la carga V3. */
	public record TramoPrototipo(long id, long desde, long hasta, double metros, Condicion condicion) {
	}

	public static GrafoPrototipo cargar() {
		List<Punto> puntos = leerCsv("/prototipo/puntos-prototipo.csv").stream().map(c -> new Punto(
				Long.parseLong(c[0]), c[1], TipoPunto.valueOf(c[2]), c[3].isEmpty() ? null : c[3],
				Boolean.parseBoolean(c[4]), Double.parseDouble(c[5]), Double.parseDouble(c[6])))
			.toList();
		List<String[]> filas = leerCsv("/prototipo/tramos-prototipo.csv");
		List<TramoPrototipo> tramos = new java.util.ArrayList<>();
		for (int i = 0; i < filas.size(); i++) {
			String[] c = filas.get(i);
			tramos.add(new TramoPrototipo(i + 1, Long.parseLong(c[0]), Long.parseLong(c[1]), Double.parseDouble(c[2]),
					Condicion.valueOf(c[3])));
		}
		return new GrafoPrototipo(puntos, List.copyOf(tramos));
	}

	/** Copia del grafo con otra condición en el tramo que une a y b (en cualquier orden). */
	public GrafoPrototipo conCondicion(long a, long b, Condicion condicion) {
		return new GrafoPrototipo(puntos, tramos.stream()
			.map(t -> (t.desde() == a && t.hasta() == b) || (t.desde() == b && t.hasta() == a)
					? new TramoPrototipo(t.id(), t.desde(), t.hasta(), t.metros(), condicion) : t)
			.toList());
	}

	public Grafo grafo() {
		Grafo.Constructor constructor = Grafo.constructor(new CalculadoraPesos());
		puntos.forEach(constructor::punto);
		tramos.forEach(t -> constructor.tramo(t.id(), t.desde(), t.hasta(), t.condicion(), t.metros()));
		return constructor.construir();
	}

	/** El grafo con el formato JSON de GET /interno/grafo de servicio-negocio. */
	public String json() {
		String jsonPuntos = puntos.stream()
			.map(p -> String.format(Locale.ROOT,
					"{\"id\":%d,\"nombre\":%s,\"tipo\":\"%s\",\"edificio\":%s,\"seleccionable\":%b,\"lat\":%s,\"lng\":%s}",
					p.id(), texto(p.nombre()), p.tipo(), p.edificio() == null ? "null" : texto(p.edificio()),
					p.seleccionable(), p.lat(), p.lng()))
			.collect(Collectors.joining(","));
		String jsonTramos = tramos.stream()
			.map(t -> String.format(Locale.ROOT,
					"{\"id\":%d,\"puntoInicialId\":%d,\"puntoFinalId\":%d,\"condicion\":\"%s\",\"longitudM\":%s}", t.id(),
					t.desde(), t.hasta(), t.condicion(), t.metros()))
			.collect(Collectors.joining(","));
		return "{\"puntos\":[" + jsonPuntos + "],\"tramos\":[" + jsonTramos + "]}";
	}

	private static String texto(String valor) {
		return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	private static List<String[]> leerCsv(String recurso) {
		try (InputStream entrada = GrafoPrototipo.class.getResourceAsStream(recurso)) {
			return new String(entrada.readAllBytes(), StandardCharsets.UTF_8).lines()
				.skip(1) // encabezado
				.filter(linea -> !linea.isBlank())
				.map(linea -> linea.split(",", -1))
				.toList();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
