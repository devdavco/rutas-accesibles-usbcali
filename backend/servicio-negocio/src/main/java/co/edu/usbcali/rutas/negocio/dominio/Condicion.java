package co.edu.usbcali.rutas.negocio.dominio;

/** Condición física de un tramo para una persona en silla de ruedas. */
public enum Condicion {
	PLANO,
	RAMPA,
	PENDIENTE_FUERTE,
	/** No se puede recorrer en silla de ruedas: el tramo se excluye del grafo. */
	ESCALERAS
}
