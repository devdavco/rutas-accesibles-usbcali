package co.edu.usbcali.rutas.grafos.dominio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.TreeMap;

/**
 * Grafo ponderado y no dirigido del campus, representado como lista de adyacencia:
 * para cada punto se guarda la lista de aristas hacia sus vecinos.
 *
 * Los tramos con escaleras no se agregan: para el algoritmo simplemente no existen.
 * Se usan TreeMap para que el recorrido de los puntos siempre siga el orden de los ids.
 */
public final class Grafo {

	private final Map<Long, Punto> puntos;

	private final Map<Long, List<Arista>> adyacencia;

	private Grafo(Map<Long, Punto> puntos, Map<Long, List<Arista>> adyacencia) {
		this.puntos = puntos;
		this.adyacencia = adyacencia;
	}

	public static Constructor constructor(CalculadoraPesos calculadoraPesos) {
		return new Constructor(calculadoraPesos);
	}

	public boolean contiene(long puntoId) {
		return puntos.containsKey(puntoId);
	}

	public Punto punto(long puntoId) {
		Punto punto = puntos.get(puntoId);
		if (punto == null) {
			throw new IllegalArgumentException("El punto " + puntoId + " no existe en el grafo");
		}
		return punto;
	}

	public Collection<Punto> puntos() {
		return Collections.unmodifiableCollection(puntos.values());
	}

	/** Aristas que salen de un punto, en el orden en que se agregaron los tramos. */
	public List<Arista> vecinos(long puntoId) {
		return Collections.unmodifiableList(adyacencia.getOrDefault(puntoId, List.of()));
	}

	/** Arma el grafo agregando primero los puntos y luego los tramos. */
	public static final class Constructor {

		private final CalculadoraPesos calculadoraPesos;

		private final Map<Long, Punto> puntos = new TreeMap<>();

		private final Map<Long, List<Arista>> adyacencia = new TreeMap<>();

		private Constructor(CalculadoraPesos calculadoraPesos) {
			this.calculadoraPesos = calculadoraPesos;
		}

		public Constructor punto(Punto punto) {
			puntos.put(punto.id(), punto);
			adyacencia.putIfAbsent(punto.id(), new ArrayList<>());
			return this;
		}

		/**
		 * Agrega un tramo en ambos sentidos (de A hacia B y de B hacia A).
		 * Si el tramo tiene escaleras, no se agrega.
		 */
		public Constructor tramo(long tramoId, long puntoA, long puntoB, Condicion condicion, double metros) {
			if (!puntos.containsKey(puntoA) || !puntos.containsKey(puntoB)) {
				throw new IllegalArgumentException("El tramo " + tramoId + " usa un punto que no existe");
			}
			OptionalDouble peso = calculadoraPesos.peso(condicion, metros);
			if (peso.isEmpty()) {
				return this; // escaleras: el tramo se descarta
			}
			adyacencia.get(puntoA).add(new Arista(puntoB, tramoId, metros, peso.getAsDouble(), condicion));
			adyacencia.get(puntoB).add(new Arista(puntoA, tramoId, metros, peso.getAsDouble(), condicion));
			return this;
		}

		public Grafo construir() {
			return new Grafo(puntos, adyacencia);
		}

	}

}
