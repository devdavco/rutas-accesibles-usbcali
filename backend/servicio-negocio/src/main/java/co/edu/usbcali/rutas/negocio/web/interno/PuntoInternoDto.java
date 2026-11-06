package co.edu.usbcali.rutas.negocio.web.interno;

import co.edu.usbcali.rutas.negocio.dominio.Punto;
import co.edu.usbcali.rutas.negocio.dominio.TipoPunto;

/** Punto del grafo tal como lo recibe servicio-grafos. */
public record PuntoInternoDto(Long id, String nombre, TipoPunto tipo, String edificio, boolean seleccionable,
		double lat, double lng) {

	static PuntoInternoDto desde(Punto punto) {
		// En la geometría, x es la longitud y y es la latitud
		return new PuntoInternoDto(punto.getId(), punto.getNombre(), punto.getTipo(), punto.getEdificio(),
				punto.isSeleccionable(), punto.getUbicacion().getY(), punto.getUbicacion().getX());
	}

}
