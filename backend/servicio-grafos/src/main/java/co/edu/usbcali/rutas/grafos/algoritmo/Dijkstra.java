package co.edu.usbcali.rutas.grafos.algoritmo;

import co.edu.usbcali.rutas.grafos.dominio.Arista;
import co.edu.usbcali.rutas.grafos.dominio.Grafo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/**
 * Algoritmo de Dijkstra escrito a mano, sin librerías de grafos.
 *
 * <p>Idea general:
 * <ul>
 *   <li>Cada punto tiene un <b>costo acumulado</b>: lo más barato que se conoce hasta ahora para
 *       llegar a él desde el origen. Al empezar solo se conoce el del origen, que vale 0; el resto
 *       vale infinito (en el código: no aparece en el mapa de costos).</li>
 *   <li>En cada ronda se toma el punto pendiente con menor costo acumulado. Como todos los pesos
 *       son positivos, ese costo ya no puede mejorar: el punto queda <b>definitivo</b>.</li>
 *   <li>Desde ese punto se revisan sus vecinos: si llegar a un vecino pasando por él es más barato
 *       que lo conocido, se actualiza su costo y se anota su <b>predecesor</b> (desde qué punto se
 *       llega). A esto se le llama "relajar" la arista.</li>
 *   <li>Se repite hasta que el destino queda definitivo (hay ruta) o no quedan puntos pendientes
 *       alcanzables (no hay ruta).</li>
 * </ul>
 *
 * <p>Para encontrar rápido el punto pendiente más barato se usa una cola de prioridad
 * ({@link PriorityQueue}). Java no permite bajar la prioridad de un elemento que ya está en la
 * cola, así que cuando un costo mejora se inserta una entrada nueva y la vieja se descarta al
 * sacarla (entrada "obsoleta").
 *
 * <p>Desempate: si dos puntos pendientes tienen el mismo costo, se elige primero el de menor id.
 * Así el resultado es siempre el mismo para los mismos datos.
 */
@Component
public class Dijkstra {

	/**
	 * Entrada de la cola de prioridad: un punto con el costo que tenía al insertarlo.
	 */
	private record EntradaCola(long puntoId, double costo) {
	}

	/** Orden de la cola: primero el menor costo y, ante empate, el menor id. */
	private static final Comparator<EntradaCola> MENOR_COSTO_Y_MENOR_ID =
			Comparator.comparingDouble(EntradaCola::costo).thenComparingLong(EntradaCola::puntoId);

	/**
	 * Busca la ruta de menor costo entre origen y destino.
	 *
	 * @param grafo grafo del campus (ya sin tramos de escaleras)
	 * @param origen id del punto de partida
	 * @param destino id del punto de llegada
	 * @param registrarPasos si se guarda cada paso para la animación del frontend
	 */
	public ResultadoDijkstra calcular(Grafo grafo, long origen, long destino, boolean registrarPasos) {
		// Se valida que ambos puntos existan (lanza IllegalArgumentException si no)
		grafo.punto(origen);
		grafo.punto(destino);

		// --- 1. Inicialización ---------------------------------------------------------------

		// Costo acumulado conocido para cada punto. Un punto ausente tiene costo infinito.
		Map<Long, Double> costos = new HashMap<>();

		// Para cada punto alcanzado: desde qué punto se llega a él y por cuál arista.
		Map<Long, Long> predecesores = new HashMap<>();
		Map<Long, Arista> aristaDeLlegada = new HashMap<>();

		// Puntos cuyo costo ya es el mínimo posible. TreeSet los mantiene ordenados por id.
		Set<Long> definitivos = new TreeSet<>();

		// Cola de puntos pendientes, ordenada por costo y luego por id.
		PriorityQueue<EntradaCola> cola = new PriorityQueue<>(MENOR_COSTO_Y_MENOR_ID);

		// El origen cuesta 0 y es el primer punto pendiente.
		costos.put(origen, 0.0);
		cola.add(new EntradaCola(origen, 0.0));

		RegistroPasos registro = new RegistroPasos(grafo, registrarPasos);
		RegistroPasos.Estado estado = new RegistroPasos.Estado(definitivos, costos, predecesores);
		registro.inicio(origen, estado);

		// --- 2. Ciclo principal: mientras queden puntos pendientes ---------------------------
		while (!cola.isEmpty()) {

			// a) Sacar de la cola el punto pendiente con menor costo (ante empate, menor id).
			EntradaCola entrada = cola.poll();
			long actual = entrada.puntoId();

			// b) Descartar entradas obsoletas: el punto ya es definitivo, o después de insertar
			//    esta entrada se encontró un camino más barato (que tiene su propia entrada).
			if (definitivos.contains(actual) || entrada.costo() > costos.get(actual)) {
				continue;
			}

			// c) El punto queda definitivo: ningún camino futuro puede ser más barato, porque
			//    cualquier otro camino pasaría por puntos pendientes que ya cuestan lo mismo o más.
			definitivos.add(actual);
			registro.seleccion(actual, estado);

			// d) Si el punto definitivo es el destino, ya tenemos la ruta de menor costo.
			if (actual == destino) {
				return rutaEncontrada(origen, destino, costos, predecesores, aristaDeLlegada, registro, estado);
			}

			// e) Relajar: revisar cada vecino que todavía no sea definitivo.
			for (Arista arista : grafo.vecinos(actual)) {
				long vecino = arista.vecinoId();
				if (definitivos.contains(vecino)) {
					continue; // su costo ya es el mejor posible
				}

				double costoNuevo = costos.get(actual) + arista.peso();
				Double costoAnterior = costos.get(vecino); // nulo = infinito
				boolean mejora = costoAnterior == null || costoNuevo < costoAnterior;

				if (mejora) {
					// Se encontró un camino más barato: se actualizan costo y predecesor, y el
					// vecino entra (o vuelve a entrar) a la cola con su nuevo costo.
					costos.put(vecino, costoNuevo);
					predecesores.put(vecino, actual);
					aristaDeLlegada.put(vecino, arista);
					cola.add(new EntradaCola(vecino, costoNuevo));
				}
				// Si no mejora (igual o peor), se conserva lo conocido: ante un empate de costo
				// gana el predecesor que se encontró primero.
				registro.revision(actual, vecino, costoAnterior, costoNuevo, mejora, estado);
			}
		}

		// --- 3. La cola se vació sin llegar al destino: no hay ruta accesible ----------------
		registro.sinRuta(destino, estado);
		return new ResultadoDijkstra(false, List.of(), List.of(), Double.POSITIVE_INFINITY,
				Double.POSITIVE_INFINITY, registro.pasos());
	}

	/**
	 * Reconstruye la ruta partiendo del destino y siguiendo los predecesores hacia atrás hasta el
	 * origen (el origen no tiene predecesor). Luego invierte las listas para que queden en orden
	 * origen → destino y suma los metros reales de la ruta.
	 */
	private ResultadoDijkstra rutaEncontrada(long origen, long destino, Map<Long, Double> costos,
			Map<Long, Long> predecesores, Map<Long, Arista> aristaDeLlegada, RegistroPasos registro,
			RegistroPasos.Estado estado) {
		List<Long> ruta = new ArrayList<>();
		List<Arista> aristas = new ArrayList<>();
		long punto = destino;
		ruta.add(punto);
		while (punto != origen) {
			aristas.add(aristaDeLlegada.get(punto));
			punto = predecesores.get(punto);
			ruta.add(punto);
		}
		Collections.reverse(ruta);
		Collections.reverse(aristas);

		// Los metros pueden ser menos que el costo: la pendiente fuerte cuenta doble en el costo.
		double metros = 0;
		for (Arista arista : aristas) {
			metros += arista.metros();
		}

		registro.fin(destino, metros, estado);
		return new ResultadoDijkstra(true, List.copyOf(ruta), List.copyOf(aristas), costos.get(destino), metros,
				registro.pasos());
	}

}
