package co.edu.usbcali.rutas.grafos.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GrafoTest {

	private static Punto punto(long id) {
		return new Punto(id, "P" + id, TipoPunto.CRUCE, null, false, 0, 0);
	}

	private static Grafo.Constructor constructorConTresPuntos() {
		return Grafo.constructor(new CalculadoraPesos()).punto(punto(1)).punto(punto(2)).punto(punto(3));
	}

	@Test
	void cadaTramoSeRecorreEnAmbosSentidos() {
		Grafo grafo = constructorConTresPuntos().tramo(10, 1, 2, Condicion.PENDIENTE_FUERTE, 8).construir();

		assertThat(grafo.vecinos(1)).containsExactly(new Arista(2, 10, 8, 16, Condicion.PENDIENTE_FUERTE));
		assertThat(grafo.vecinos(2)).containsExactly(new Arista(1, 10, 8, 16, Condicion.PENDIENTE_FUERTE));
		assertThat(grafo.vecinos(3)).isEmpty();
	}

	@Test
	void losTramosConEscalerasNoEntranAlGrafo() {
		Grafo grafo = constructorConTresPuntos()
			.tramo(10, 1, 2, Condicion.ESCALERAS, 5)
			.tramo(11, 2, 3, Condicion.RAMPA, 7)
			.construir();

		assertThat(grafo.vecinos(1)).isEmpty();
		assertThat(grafo.vecinos(2)).extracting(Arista::vecinoId).containsExactly(3L);
		// El punto sigue existiendo aunque se quede sin tramos
		assertThat(grafo.contiene(1)).isTrue();
	}

	@Test
	void unTramoHaciaUnPuntoInexistenteFalla() {
		assertThatThrownBy(() -> constructorConTresPuntos().tramo(10, 1, 99, Condicion.PLANO, 5))
			.isInstanceOf(IllegalArgumentException.class);
	}

}
