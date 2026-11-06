package co.edu.usbcali.rutas.grafos.cliente;

import co.edu.usbcali.rutas.grafos.dominio.TipoPunto;

/** Punto tal como lo entrega servicio-negocio en /interno/grafo. */
public record PuntoNegocio(long id, String nombre, TipoPunto tipo, String edificio, boolean seleccionable, double lat,
		double lng) {
}
