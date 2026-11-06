package co.edu.usbcali.rutas.grafos.respuesta;

import co.edu.usbcali.rutas.grafos.dominio.TipoPunto;

/** Punto de la ruta, en el orden en que se recorre. */
public record PuntoRuta(long id, String nombre, TipoPunto tipo, double lat, double lng) {
}
