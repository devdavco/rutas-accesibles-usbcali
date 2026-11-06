package co.edu.usbcali.rutas.grafos.algoritmo;

import co.edu.usbcali.rutas.grafos.dominio.Grafo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Anota cada paso de Dijkstra con una copia del estado y un mensaje en español.
 * Si está inactivo (la petición no pidió los pasos) no copia nada.
 */
final class RegistroPasos {

	private static final Locale ESPANOL = Locale.forLanguageTag("es-CO");

	private final Grafo grafo;

	private final boolean activo;

	private final List<PasoDijkstra> pasos = new ArrayList<>();

	RegistroPasos(Grafo grafo, boolean activo) {
		this.grafo = grafo;
		this.activo = activo;
	}

	List<PasoDijkstra> pasos() {
		return List.copyOf(pasos);
	}

	void inicio(long origen, Estado estado) {
		agregar(TipoPaso.INICIO, null, null, null, null, null, estado,
				"Inicio: todos los puntos tienen costo ∞ (desconocido), excepto el origen, %s, que vale 0."
					.formatted(nombre(origen)));
	}

	void seleccion(long actual, Estado estado) {
		agregar(TipoPaso.SELECCION, actual, null, null, null, null, estado,
				"Se elige %s: es el punto pendiente con menor costo (%s). Su costo queda definitivo."
					.formatted(nombre(actual), formatear(estado.costos().get(actual))));
	}

	void revision(long actual, long vecino, Double costoAnterior, double costoNuevo, boolean actualizado,
			Estado estado) {
		String mensaje = actualizado
				? "Desde %s se revisa %s: llegar por aquí cuesta %s, mejor que lo conocido (%s). Se actualizan su costo y su predecesor."
				: "Desde %s se revisa %s: llegar por aquí cuesta %s, no mejora lo conocido (%s). No se cambia nada.";
		agregar(TipoPaso.REVISION, actual, vecino, costoAnterior, costoNuevo, actualizado, estado,
				mensaje.formatted(nombre(actual), nombre(vecino), formatear(costoNuevo), formatear(costoAnterior)));
	}

	void fin(long destino, double metros, Estado estado) {
		agregar(TipoPaso.FIN, destino, null, null, null, null, estado,
				"Listo: %s quedó definitivo. La ruta de menor costo cuesta %s y recorre %s m. Se reconstruye siguiendo los predecesores desde el destino."
					.formatted(nombre(destino), formatear(estado.costos().get(destino)), formatear(metros)));
	}

	void sinRuta(long destino, Estado estado) {
		agregar(TipoPaso.SIN_RUTA, null, null, null, null, null, estado,
				"No quedan puntos pendientes alcanzables: no existe una ruta accesible hasta %s."
					.formatted(nombre(destino)));
	}

	private void agregar(TipoPaso tipo, Long actual, Long vecino, Double costoAnterior, Double costoNuevo,
			Boolean actualizado, Estado estado, String mensaje) {
		if (!activo) {
			return;
		}
		pasos.add(new PasoDijkstra(pasos.size() + 1, tipo, actual, vecino, redondear(costoAnterior),
				redondear(costoNuevo), actualizado, List.copyOf(estado.definitivos()), copiarCostos(estado.costos()),
				new TreeMap<>(estado.predecesores()), mensaje));
	}

	private String nombre(long puntoId) {
		return grafo.punto(puntoId).nombre();
	}

	/** Costos redondeados a centímetros: es una copia para mostrar, el algoritmo no se afecta. */
	private static Map<Long, Double> copiarCostos(Map<Long, Double> costos) {
		Map<Long, Double> copia = new TreeMap<>();
		costos.forEach((id, costo) -> copia.put(id, redondear(costo)));
		return copia;
	}

	private static Double redondear(Double valor) {
		return valor == null ? null : Math.round(valor * 100) / 100.0;
	}

	private static String formatear(Double valor) {
		return valor == null ? "∞" : String.format(ESPANOL, "%.1f", valor);
	}

	/** Vista del estado del algoritmo que se copia en cada paso. */
	record Estado(Collection<Long> definitivos, Map<Long, Double> costos, Map<Long, Long> predecesores) {
	}

}
