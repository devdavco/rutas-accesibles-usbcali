package co.edu.usbcali.rutas.grafos.algoritmo;

import co.edu.usbcali.rutas.grafos.dominio.CalculadoraPesos;
import co.edu.usbcali.rutas.grafos.dominio.Condicion;
import co.edu.usbcali.rutas.grafos.dominio.Grafo;
import co.edu.usbcali.rutas.grafos.dominio.Punto;
import co.edu.usbcali.rutas.grafos.dominio.TipoPunto;

/** Arma grafos pequeños para las pruebas: puntos P1..Pn y tramos numerados en orden. */
final class GrafosDePrueba {

	private final Grafo.Constructor constructor = Grafo.constructor(new CalculadoraPesos());

	private long siguienteTramo = 1;

	private GrafosDePrueba(long... ids) {
		for (long id : ids) {
			constructor.punto(new Punto(id, "P" + id, TipoPunto.CRUCE, null, false, 0, 0));
		}
	}

	static GrafosDePrueba conPuntos(long... ids) {
		return new GrafosDePrueba(ids);
	}

	GrafosDePrueba plano(long a, long b, double metros) {
		return tramo(a, b, Condicion.PLANO, metros);
	}

	GrafosDePrueba tramo(long a, long b, Condicion condicion, double metros) {
		constructor.tramo(siguienteTramo++, a, b, condicion, metros);
		return this;
	}

	Grafo construir() {
		return constructor.construir();
	}

	/**
	 * Grafo clásico de 6 puntos (el ejemplo de Dijkstra de Wikipedia):
	 * <pre>
	 *   1-2: 7   1-3: 9   1-6: 14   2-3: 10   2-4: 15
	 *   3-4: 11  3-6: 2   4-5: 6    5-6: 9
	 * </pre>
	 * Costos mínimos desde 1: P2=7, P3=9, P6=11, P4=20, P5=20 (1→3→6→5).
	 */
	static Grafo clasicoDeSeisPuntos() {
		return conPuntos(1, 2, 3, 4, 5, 6)
			.plano(1, 2, 7).plano(1, 3, 9).plano(1, 6, 14)
			.plano(2, 3, 10).plano(2, 4, 15)
			.plano(3, 4, 11).plano(3, 6, 2)
			.plano(4, 5, 6).plano(5, 6, 9)
			.construir();
	}

}
