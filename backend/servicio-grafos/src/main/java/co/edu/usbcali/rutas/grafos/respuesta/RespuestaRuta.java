package co.edu.usbcali.rutas.grafos.respuesta;

import co.edu.usbcali.rutas.grafos.algoritmo.PasoDijkstra;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Respuesta de GET /api/rutas.
 *
 * @param encontrada si existe una ruta accesible
 * @param mensaje explicación corta del resultado
 * @param distanciaM metros reales de la ruta (nulo si no hay ruta)
 * @param costo suma de pesos de la ruta (nulo si no hay ruta)
 * @param puntos puntos de la ruta en orden de recorrido
 * @param segmentos tramos consecutivos agrupados por condición (RF05)
 * @param geojson la ruta como FeatureCollection: LineString del recorrido y Point de origen y destino
 * @param pasos pasos del algoritmo, solo si se pidieron con pasos=true
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespuestaRuta(boolean encontrada, String mensaje, ReferenciaPunto origen, ReferenciaPunto destino,
		Double distanciaM, Double costo, List<PuntoRuta> puntos, List<Segmento> segmentos,
		GeoJson.ColeccionElementos geojson, List<PasoDijkstra> pasos) {
}
