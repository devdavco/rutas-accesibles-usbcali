package co.edu.usbcali.rutas.negocio.servicio;

/** El recurso pedido no existe (404). */
public class RecursoNoEncontradoException extends RuntimeException {

	public RecursoNoEncontradoException(String mensaje) {
		super(mensaje);
	}

}
