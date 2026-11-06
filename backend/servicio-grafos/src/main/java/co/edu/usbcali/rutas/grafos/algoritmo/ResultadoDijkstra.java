package co.edu.usbcali.rutas.grafos.algoritmo;

import co.edu.usbcali.rutas.grafos.dominio.Arista;
import java.util.List;

/**
 * Resultado de una ejecución de Dijkstra.
 *
 * @param encontrada si existe una ruta accesible
 * @param ruta ids de los puntos en orden origen → destino (vacía si no hay ruta)
 * @param aristas arista usada para pasar de cada punto de la ruta al siguiente (una menos que puntos)
 * @param costo suma de los pesos de la ruta (metros × factor)
 * @param metros suma de las longitudes reales de la ruta
 * @param pasos pasos registrados (vacía si no se pidió registrarlos)
 */
public record ResultadoDijkstra(boolean encontrada, List<Long> ruta, List<Arista> aristas, double costo, double metros,
		List<PasoDijkstra> pasos) {
}
