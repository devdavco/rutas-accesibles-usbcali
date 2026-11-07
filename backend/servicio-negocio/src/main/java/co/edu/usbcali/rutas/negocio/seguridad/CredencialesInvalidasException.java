package co.edu.usbcali.rutas.negocio.seguridad;

/** Correo o contraseña incorrectos (el mensaje no dice cuál de los dos). */
public class CredencialesInvalidasException extends RuntimeException {

	public CredencialesInvalidasException() {
		super("Correo o contraseña incorrectos.");
	}

}
