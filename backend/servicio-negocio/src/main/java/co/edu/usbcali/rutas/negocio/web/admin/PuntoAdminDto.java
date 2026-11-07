package co.edu.usbcali.rutas.negocio.web.admin;

import co.edu.usbcali.rutas.negocio.dominio.Punto;
import co.edu.usbcali.rutas.negocio.dominio.TipoPunto;

/** Punto tal como lo ve y edita el administrador. */
public record PuntoAdminDto(Long id, String nombre, TipoPunto tipo, String edificio, boolean seleccionable, double lat,
		double lng) {

	public static PuntoAdminDto desde(Punto punto) {
		// En la geometría, x es la longitud y y es la latitud
		return new PuntoAdminDto(punto.getId(), punto.getNombre(), punto.getTipo(), punto.getEdificio(),
				punto.isSeleccionable(), punto.getUbicacion().getY(), punto.getUbicacion().getX());
	}

}
