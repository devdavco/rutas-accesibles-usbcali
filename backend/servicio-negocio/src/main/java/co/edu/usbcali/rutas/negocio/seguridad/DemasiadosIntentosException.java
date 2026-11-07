package co.edu.usbcali.rutas.negocio.seguridad;

/** El correo está bloqueado temporalmente por ingresos fallidos. */
public class DemasiadosIntentosException extends RuntimeException {

	public DemasiadosIntentosException() {
		super("Demasiados intentos. Intente de nuevo en unos minutos.");
	}

}
