package co.edu.usbcali.rutas.grafos.algoritmo;

/** Qué hizo el algoritmo en un paso registrado. */
public enum TipoPaso {
	/** Estado inicial: solo el origen tiene costo conocido (0). */
	INICIO,
	/** Se sacó de la cola el punto pendiente más barato y quedó definitivo. */
	SELECCION,
	/** Se revisó un vecino del punto actual (puede mejorar o no su costo). */
	REVISION,
	/** El destino quedó definitivo: hay ruta. */
	FIN,
	/** La cola se vació sin llegar al destino: no hay ruta accesible. */
	SIN_RUTA
}
