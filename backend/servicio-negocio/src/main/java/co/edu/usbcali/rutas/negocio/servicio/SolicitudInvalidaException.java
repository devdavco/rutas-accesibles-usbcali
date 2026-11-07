package co.edu.usbcali.rutas.negocio.servicio;

/** Los datos enviados no cumplen las reglas del grafo (400). */
public class SolicitudInvalidaException extends RuntimeException {

	public SolicitudInvalidaException(String mensaje) {
		super(mensaje);
	}

}
