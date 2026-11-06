package co.edu.usbcali.rutas.grafos.algoritmo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.usbcali.rutas.grafos.dominio.Condicion;
import co.edu.usbcali.rutas.grafos.dominio.Grafo;
import org.junit.jupiter.api.Test;

/** Casos pequeños cuyo resultado se calculó a mano (ver el dibujo de cada grafo). */
class DijkstraTest {

	private final Dijkstra dijkstra = new Dijkstra();

	@Test
	void rutaUnica() {
		// 1 —10— 2 —5 (rampa)— 3 —7— 4 : solo hay un camino, 22 m
		Grafo grafo = GrafosDePrueba.conPuntos(1, 2, 3, 4)
			.plano(1, 2, 10).tramo(2, 3, Condicion.RAMPA, 5).plano(3, 4, 7)
			.construir();

		ResultadoDijkstra r = dijkstra.calcular(grafo, 1, 4, false);

		assertThat(r.encontrada()).isTrue();
		assertThat(r.ruta()).containsExactly(1L, 2L, 3L, 4L);
		assertThat(r.metros()).isEqualTo(22.0);
		assertThat(r.costo()).isEqualTo(22.0);
		assertThat(r.aristas()).extracting(a -> a.condicion())
			.containsExactly(Condicion.PLANO, Condicion.RAMPA, Condicion.PLANO);
	}

	@Test
	void grafoClasicoDeSeisPuntos() {
		// 1→5: 1→3 (9) →6 (2) →5 (9) = 20, más barato que 1→3→4→5 (26) o 1→6→5 (23)
		ResultadoDijkstra r = dijkstra.calcular(GrafosDePrueba.clasicoDeSeisPuntos(), 1, 5, false);

		assertThat(r.ruta()).containsExactly(1L, 3L, 6L, 5L);
		assertThat(r.costo()).isEqualTo(20.0);
	}

	@Test
	void anteDosRutasDeIgualCostoGanaLaQuePasaPorElMenorId() {
		//      5           Las dos rutas cuestan 10. P3 y P5 empatan con costo 5:
		//    /   \         se elige primero P3 (menor id), que llega a P4 con 10.
		//   1     4        Cuando se revisa P5, llegar a P4 también cuesta 10: no mejora,
		//    \   /         así que P4 conserva a P3 como predecesor.
		//      3           El tramo por P5 se agrega primero a propósito: el orden no influye.
		Grafo grafo = GrafosDePrueba.conPuntos(1, 3, 4, 5)
			.plano(1, 5, 5).plano(5, 4, 5)
			.plano(1, 3, 5).plano(3, 4, 5)
			.construir();

		ResultadoDijkstra r = dijkstra.calcular(grafo, 1, 4, false);

		assertThat(r.ruta()).containsExactly(1L, 3L, 4L);
		assertThat(r.costo()).isEqualTo(10.0);
	}

	@Test
	void destinoInalcanzable() {
		// 1 —5— 2      3 —4— 4 : P3 está en otra componente
		Grafo grafo = GrafosDePrueba.conPuntos(1, 2, 3, 4).plano(1, 2, 5).plano(3, 4, 4).construir();

		ResultadoDijkstra r = dijkstra.calcular(grafo, 1, 3, false);

		assertThat(r.encontrada()).isFalse();
		assertThat(r.ruta()).isEmpty();
		assertThat(r.aristas()).isEmpty();
		assertThat(r.costo()).isInfinite();
	}

	@Test
	void origenIgualADestino() {
		ResultadoDijkstra r = dijkstra.calcular(GrafosDePrueba.clasicoDeSeisPuntos(), 4, 4, false);

		assertThat(r.encontrada()).isTrue();
		assertThat(r.ruta()).containsExactly(4L);
		assertThat(r.aristas()).isEmpty();
		assertThat(r.costo()).isZero();
		assertThat(r.metros()).isZero();
	}

	@Test
	void excluirEscalerasObligaAUnaRutaMasLarga() {
		//   1 —10 (escaleras)— 2      Sin escaleras serían 10 m. Como el tramo se excluye,
		//    \               /        hay que rodear por P3: 20 + 20 = 40 m.
		//     20 —— 3 —— 20
		Grafo grafo = GrafosDePrueba.conPuntos(1, 2, 3)
			.tramo(1, 2, Condicion.ESCALERAS, 10).plano(1, 3, 20).plano(3, 2, 20)
			.construir();

		ResultadoDijkstra r = dijkstra.calcular(grafo, 1, 2, false);

		assertThat(r.ruta()).containsExactly(1L, 3L, 2L);
		assertThat(r.metros()).isEqualTo(40.0);
	}

	@Test
	void laPendienteFuerteHacePreferirUnCaminoMasLargoEnMetros() {
		//   1 —30 m (pendiente fuerte, costo 60)— 2
		//    \                                  /
		//     25 m —————— 3 —————— 25 m          → rodeo: 50 m y costo 50
		// El rodeo tiene más metros (50 > 30) pero menor costo (50 < 60).
		Grafo grafo = GrafosDePrueba.conPuntos(1, 2, 3)
			.tramo(1, 2, Condicion.PENDIENTE_FUERTE, 30).plano(1, 3, 25).plano(3, 2, 25)
			.construir();

		ResultadoDijkstra r = dijkstra.calcular(grafo, 1, 2, false);

		assertThat(r.ruta()).containsExactly(1L, 3L, 2L);
		assertThat(r.metros()).isEqualTo(50.0);
		assertThat(r.costo()).isEqualTo(50.0);
	}

	@Test
	void unaPendienteFuerteCortaSigueConviniendoYElCostoDifiereDeLosMetros() {
		// Igual que el anterior, pero la pendiente mide 20 m (costo 40 < 50 del rodeo)
		Grafo grafo = GrafosDePrueba.conPuntos(1, 2, 3)
			.tramo(1, 2, Condicion.PENDIENTE_FUERTE, 20).plano(1, 3, 25).plano(3, 2, 25)
			.construir();

		ResultadoDijkstra r = dijkstra.calcular(grafo, 1, 2, false);

		assertThat(r.ruta()).containsExactly(1L, 2L);
		assertThat(r.metros()).isEqualTo(20.0);
		assertThat(r.costo()).isEqualTo(40.0);
	}

	@Test
	void sinRegistrarPasosLaListaDePasosEstaVacia() {
		ResultadoDijkstra r = dijkstra.calcular(GrafosDePrueba.clasicoDeSeisPuntos(), 1, 5, false);

		assertThat(r.pasos()).isEmpty();
	}

	@Test
	void unPuntoInexistenteFalla() {
		assertThatThrownBy(() -> dijkstra.calcular(GrafosDePrueba.clasicoDeSeisPuntos(), 1, 99, false))
			.isInstanceOf(IllegalArgumentException.class);
	}

}
