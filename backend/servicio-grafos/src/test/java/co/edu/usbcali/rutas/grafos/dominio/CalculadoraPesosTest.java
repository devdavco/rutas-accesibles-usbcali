package co.edu.usbcali.rutas.grafos.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CalculadoraPesosTest {

	private final CalculadoraPesos calculadora = new CalculadoraPesos();

	@Test
	void planoTieneFactorUno() {
		assertThat(calculadora.peso(Condicion.PLANO, 12.5)).hasValue(12.5);
	}

	@Test
	void rampaTieneFactorUno() {
		assertThat(calculadora.peso(Condicion.RAMPA, 12.5)).hasValue(12.5);
	}

	@Test
	void pendienteFuerteTieneFactorDos() {
		assertThat(calculadora.peso(Condicion.PENDIENTE_FUERTE, 12.5)).hasValue(25.0);
	}

	@Test
	void escalerasNoTienenPeso() {
		assertThat(calculadora.peso(Condicion.ESCALERAS, 12.5)).isEmpty();
	}

}
