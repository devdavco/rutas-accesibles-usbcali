package co.edu.usbcali.rutas.grafos.servicio;

import co.edu.usbcali.rutas.grafos.algoritmo.ResultadoDijkstra;
import co.edu.usbcali.rutas.grafos.dominio.Arista;
import co.edu.usbcali.rutas.grafos.dominio.Grafo;
import co.edu.usbcali.rutas.grafos.dominio.Punto;
import co.edu.usbcali.rutas.grafos.respuesta.GeoJson;
import co.edu.usbcali.rutas.grafos.respuesta.PuntoRuta;
import co.edu.usbcali.rutas.grafos.respuesta.ReferenciaPunto;
import co.edu.usbcali.rutas.grafos.respuesta.RespuestaRuta;
import co.edu.usbcali.rutas.grafos.respuesta.Segmento;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Convierte el resultado de Dijkstra en la respuesta del API (puntos, segmentos y GeoJSON). */
@Component
public class ConstructorRespuesta {

	private static final Locale ESPANOL = Locale.forLanguageTag("es-CO");

	public RespuestaRuta construir(Grafo grafo, long origenId, long destinoId, ResultadoDijkstra resultado,
			boolean incluirPasos) {
		Punto origen = grafo.punto(origenId);
		Punto destino = grafo.punto(destinoId);
		var pasos = incluirPasos ? resultado.pasos() : null;

		if (!resultado.encontrada()) {
			String mensaje = "No existe una ruta accesible en silla de ruedas entre %s y %s."
				.formatted(origen.nombre(), destino.nombre());
			return new RespuestaRuta(false, mensaje, referencia(origen), referencia(destino), null, null, List.of(),
					List.of(), null, pasos);
		}

		List<Punto> puntos = resultado.ruta().stream().map(grafo::punto).toList();
		String mensaje = origenId == destinoId ? "El origen y el destino son el mismo punto."
				: String.format(ESPANOL, "Ruta accesible encontrada: %.1f m.", resultado.metros());
		return new RespuestaRuta(true, mensaje, referencia(origen), referencia(destino),
				redondear(resultado.metros()), redondear(resultado.costo()),
				puntos.stream().map(p -> new PuntoRuta(p.id(), p.nombre(), p.tipo(), p.lat(), p.lng())).toList(),
				agruparSegmentos(puntos, resultado.aristas()), geoJson(puntos, origen, destino, resultado.metros()),
				pasos);
	}

	/**
	 * Une los tramos consecutivos que tienen la misma condición. Por ejemplo, plano-plano-rampa-plano
	 * queda como tres segmentos: plano, rampa y plano.
	 */
	List<Segmento> agruparSegmentos(List<Punto> puntos, List<Arista> aristas) {
		List<Segmento> segmentos = new ArrayList<>();
		int inicio = 0; // índice del punto donde empieza el segmento en curso
		double metros = 0;
		for (int i = 0; i < aristas.size(); i++) {
			metros += aristas.get(i).metros();
			boolean ultimo = i == aristas.size() - 1;
			if (ultimo || aristas.get(i + 1).condicion() != aristas.get(i).condicion()) {
				Punto desde = puntos.get(inicio);
				Punto hasta = puntos.get(i + 1);
				segmentos.add(new Segmento(aristas.get(i).condicion(), redondear(metros), desde.id(), desde.nombre(),
						hasta.id(), hasta.nombre()));
				inicio = i + 1;
				metros = 0;
			}
		}
		return segmentos;
	}

	/**
	 * FeatureCollection con la línea del recorrido y los puntos de origen y destino.
	 * Si origen y destino coinciden no hay línea: un LineString necesita al menos dos posiciones.
	 */
	private GeoJson.ColeccionElementos geoJson(List<Punto> puntos, Punto origen, Punto destino, double metros) {
		List<GeoJson.Elemento> elementos = new ArrayList<>();
		if (puntos.size() >= 2) {
			List<List<Double>> coordenadas = puntos.stream().map(p -> List.of(p.lng(), p.lat())).toList();
			elementos.add(new GeoJson.Elemento(GeoJson.Geometria.linea(coordenadas),
					propiedades("ruta", null, null, redondear(metros))));
		}
		elementos.add(new GeoJson.Elemento(GeoJson.Geometria.punto(origen.lat(), origen.lng()),
				propiedades("origen", origen.id(), origen.nombre(), null)));
		elementos.add(new GeoJson.Elemento(GeoJson.Geometria.punto(destino.lat(), destino.lng()),
				propiedades("destino", destino.id(), destino.nombre(), null)));
		return new GeoJson.ColeccionElementos(elementos);
	}

	private static Map<String, Object> propiedades(String tipo, Long id, String nombre, Double distanciaM) {
		Map<String, Object> propiedades = new LinkedHashMap<>();
		propiedades.put("tipo", tipo);
		if (id != null) {
			propiedades.put("id", id);
			propiedades.put("nombre", nombre);
		}
		if (distanciaM != null) {
			propiedades.put("distanciaM", distanciaM);
		}
		return propiedades;
	}

	private static ReferenciaPunto referencia(Punto punto) {
		return new ReferenciaPunto(punto.id(), punto.nombre());
	}

	/** Redondea a centímetros para la respuesta. */
	private static double redondear(double metros) {
		return Math.round(metros * 100) / 100.0;
	}

}
