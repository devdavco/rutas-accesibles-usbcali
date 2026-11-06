package co.edu.usbcali.rutas.negocio.dominio;

/** Clase de punto del grafo. */
public enum TipoPunto {
	/** Entrada al campus (por ejemplo, la entrada principal). */
	ENTRADA_CAMPUS,
	/** Entrada de un edificio; siempre indica a qué edificio pertenece. */
	ENTRADA_EDIFICIO,
	/** Cruce o giro del recorrido: solo sirve de paso. */
	CRUCE
}
