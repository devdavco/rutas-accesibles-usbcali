package co.edu.usbcali.rutas.grafos.servicio;

/** Origen o destino inexistente o no seleccionable. Se responde con 400. */
public class SolicitudRutaInvalidaException extends RuntimeException {

	public SolicitudRutaInvalidaException(String mensaje) {
		super(mensaje);
	}

}
