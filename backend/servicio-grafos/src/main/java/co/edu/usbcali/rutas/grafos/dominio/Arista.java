package co.edu.usbcali.rutas.grafos.dominio;

/**
 * Un sentido de recorrido de un tramo: desde el punto dueño de la lista de adyacencia hasta
 * {@code vecinoId}. Cada tramo genera dos aristas, una en cada sentido.
 *
 * @param vecinoId punto al que se llega
 * @param tramoId tramo del que sale la arista
 * @param metros longitud real del tramo
 * @param peso costo de recorrerlo en silla de ruedas (metros × factor de la condición)
 * @param condicion condición del tramo
 */
public record Arista(long vecinoId, long tramoId, double metros, double peso, Condicion condicion) {
}
