package co.edu.usbcali.rutas.grafos.algoritmo;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/**
 * Fotografía del estado del algoritmo en un momento dado, para la animación del frontend.
 *
 * @param numero número del paso, empezando en 1
 * @param tipo qué hizo el algoritmo en este paso
 * @param actualId punto que se está procesando (nulo en INICIO y SIN_RUTA)
 * @param vecinoId vecino revisado (solo en REVISION)
 * @param costoAnterior costo conocido del vecino antes de revisarlo; nulo significa infinito
 * @param costoNuevo costo de llegar al vecino pasando por el punto actual
 * @param actualizado si la revisión mejoró el costo del vecino (solo en REVISION)
 * @param definitivos puntos cuyo costo ya no puede cambiar, en orden de id
 * @param costos costo acumulado de cada punto con costo conocido (los ausentes valen infinito)
 * @param predecesores para cada punto alcanzado, desde qué punto se llega a él
 * @param mensaje explicación del paso en español
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PasoDijkstra(int numero, TipoPaso tipo, Long actualId, Long vecinoId, Double costoAnterior,
		Double costoNuevo, Boolean actualizado, List<Long> definitivos, Map<Long, Double> costos,
		Map<Long, Long> predecesores, String mensaje) {
}
