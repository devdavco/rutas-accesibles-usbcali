package co.edu.usbcali.rutas.grafos.respuesta;

import co.edu.usbcali.rutas.grafos.dominio.TipoPunto;

/** Punto que se puede elegir como origen o destino; la interfaz los agrupa por edificio. */
public record PuntoSeleccionable(long id, String nombre, TipoPunto tipo, String edificio) {
}
