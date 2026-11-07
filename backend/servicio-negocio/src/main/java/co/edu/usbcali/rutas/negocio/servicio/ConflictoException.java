package co.edu.usbcali.rutas.negocio.servicio;

/** La operación choca con los datos existentes (409). */
public class ConflictoException extends RuntimeException {

	public ConflictoException(String mensaje) {
		super(mensaje);
	}

}
