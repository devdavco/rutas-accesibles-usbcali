package co.edu.usbcali.rutas.grafos.dominio;

/** Vértice del grafo con los datos que necesita la respuesta (nombre y coordenadas). */
public record Punto(long id, String nombre, TipoPunto tipo, String edificio, boolean seleccionable, double lat,
		double lng) {
}
