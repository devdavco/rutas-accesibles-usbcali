package co.edu.usbcali.rutas.grafos.algoritmo;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.usbcali.rutas.grafos.dominio.Grafo;
import co.edu.usbcali.rutas.grafos.soporte.GrafoPrototipo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/** Verifica que los pasos registrados para la animación son coherentes con el resultado. */
class PasosDijkstraTest {

	private final Dijkstra dijkstra = new Dijkstra();

	@Test
	void pasosDeUnaCadenaCalculadosAMano() {
		// 1 —5— 2 —5— 3, de 1 a 3:
		//  1 INICIO · 2 SELECCION P1 · 3 REVISION P1→P2 (∞ → 5) · 4 SELECCION P2
		//  5 REVISION P2→P3 (∞ → 10) (P1 ya es definitivo, no se revisa) · 6 SELECCION P3 · 7 FIN
		Grafo grafo = GrafosDePrueba.conPuntos(1, 2, 3).plano(1, 2, 5).plano(2, 3, 5).construir();

		List<PasoDijkstra> pasos = dijkstra.calcular(grafo, 1, 3, true).pasos();

		assertThat(pasos).extracting(PasoDijkstra::tipo)
			.containsExactly(TipoPaso.INICIO, TipoPaso.SELECCION, TipoPaso.REVISION, TipoPaso.SELECCION,
					TipoPaso.REVISION, TipoPaso.SELECCION, TipoPaso.FIN);
		PasoDijkstra revision = pasos.get(2);
		assertThat(revision.actualId()).isEqualTo(1L);
		assertThat(revision.vecinoId()).isEqualTo(2L);
		assertThat(revision.costoAnterior()).isNull();
		assertThat(revision.costoNuevo()).isEqualTo(5.0);
		assertThat(revision.actualizado()).isTrue();
		assertThat(revision.mensaje()).contains("P1", "P2", "5,0", "∞");
		assertThat(pasos.getLast().definitivos()).containsExactly(1L, 2L, 3L);
		assertThat(pasos.getLast().costos()).containsEntry(3L, 10.0);
	}

	@Test
	void pasosCoherentesEnElGrafoClasico() {
		Grafo grafo = GrafosDePrueba.clasicoDeSeisPuntos();
		ResultadoDijkstra r = dijkstra.calcular(grafo, 1, 5, true);

		verificarCoherencia(r, 1, 5);
		// Los costos definitivos calculados a mano
		assertThat(r.pasos().getLast().costos()).containsEntry(2L, 7.0).containsEntry(3L, 9.0)
			.containsEntry(6L, 11.0).containsEntry(5L, 20.0);
	}

	@Test
	void pasosCoherentesEnElGrafoDelPrototipo() {
		Grafo grafo = GrafoPrototipo.cargar().grafo();
		ResultadoDijkstra conPasos = dijkstra.calcular(grafo, 1, 2, true);

		verificarCoherencia(conPasos, 1, 2);
		// Registrar pasos no cambia el resultado
		ResultadoDijkstra sinPasos = dijkstra.calcular(grafo, 1, 2, false);
		assertThat(sinPasos.ruta()).isEqualTo(conPasos.ruta());
		assertThat(sinPasos.costo()).isEqualTo(conPasos.costo());
	}

	@Test
	void pasosCoherentesCuandoNoHayRuta() {
		Grafo grafo = GrafosDePrueba.conPuntos(1, 2, 3, 4).plano(1, 2, 5).plano(3, 4, 4).construir();
		ResultadoDijkstra r = dijkstra.calcular(grafo, 1, 3, true);

		List<PasoDijkstra> pasos = r.pasos();
		assertThat(pasos.getFirst().tipo()).isEqualTo(TipoPaso.INICIO);
		assertThat(pasos.getLast().tipo()).isEqualTo(TipoPaso.SIN_RUTA);
		// Se agotó la componente del origen: P1 y P2 quedaron definitivos, P3 nunca tuvo costo
		assertThat(pasos.getLast().definitivos()).containsExactly(1L, 2L);
		assertThat(pasos.getLast().costos()).doesNotContainKey(3L);
		verificarNumeracionYSelecciones(pasos);
	}

	/** Reglas que debe cumplir cualquier ejecución con ruta. */
	private static void verificarCoherencia(ResultadoDijkstra r, long origen, long destino) {
		List<PasoDijkstra> pasos = r.pasos();
		assertThat(r.encontrada()).isTrue();
		assertThat(pasos.getFirst().tipo()).isEqualTo(TipoPaso.INICIO);
		assertThat(pasos.getFirst().costos()).containsOnlyKeys(origen);
		assertThat(pasos.getLast().tipo()).isEqualTo(TipoPaso.FIN);
		verificarNumeracionYSelecciones(pasos);

		PasoDijkstra ultimo = pasos.getLast();
		// El costo final del destino es el costo de la ruta
		assertThat(ultimo.costos().get(destino)).isEqualTo(Math.round(r.costo() * 100) / 100.0);
		assertThat(ultimo.definitivos()).contains(destino);

		// Seguir los predecesores del último paso desde el destino reproduce la ruta
		List<Long> ruta = new ArrayList<>();
		Long punto = destino;
		while (punto != null) {
			ruta.add(punto);
			punto = ultimo.predecesores().get(punto);
		}
		Collections.reverse(ruta);
		assertThat(ruta).isEqualTo(r.ruta());

		// Cada revisión es coherente con su decisión
		for (PasoDijkstra paso : pasos) {
			if (paso.tipo() != TipoPaso.REVISION) {
				continue;
			}
			assertThat(paso.definitivos()).as("paso %d", paso.numero()).doesNotContain(paso.vecinoId());
			if (paso.actualizado()) {
				assertThat(paso.costoAnterior() == null || paso.costoNuevo() < paso.costoAnterior())
					.as("paso %d mejora", paso.numero()).isTrue();
				assertThat(paso.costos().get(paso.vecinoId())).isEqualTo(paso.costoNuevo());
				assertThat(paso.predecesores().get(paso.vecinoId())).isEqualTo(paso.actualId());
			}
			else {
				assertThat(paso.costoNuevo()).as("paso %d no mejora", paso.numero())
					.isGreaterThanOrEqualTo(paso.costoAnterior());
			}
		}
	}

	/**
	 * Los pasos van numerados 1..n, cada SELECCION agrega exactamente su punto a los definitivos y
	 * hay tantas selecciones como definitivos al final.
	 */
	private static void verificarNumeracionYSelecciones(List<PasoDijkstra> pasos) {
		TreeSet<Long> definitivos = new TreeSet<>();
		int selecciones = 0;
		for (int i = 0; i < pasos.size(); i++) {
			PasoDijkstra paso = pasos.get(i);
			assertThat(paso.numero()).isEqualTo(i + 1);
			assertThat(paso.mensaje()).isNotBlank();
			if (paso.tipo() == TipoPaso.SELECCION) {
				selecciones++;
				assertThat(definitivos).doesNotContain(paso.actualId());
				definitivos.add(paso.actualId());
			}
			assertThat(paso.definitivos()).containsExactlyElementsOf(definitivos);
		}
		assertThat(selecciones).isEqualTo(pasos.getLast().definitivos().size());
	}

}
