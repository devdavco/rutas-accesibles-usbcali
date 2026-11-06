package co.edu.usbcali.rutas.grafos.dominio;

import java.util.OptionalDouble;
import org.springframework.stereotype.Component;

/**
 * Calcula el peso (costo) de recorrer un tramo en silla de ruedas.
 *
 * <pre>
 *   peso = metros × factor de la condición
 *   PLANO            → factor 1
 *   RAMPA            → factor 1
 *   PENDIENTE_FUERTE → factor 2 (cuesta el doble de esfuerzo)
 *   ESCALERAS        → sin peso: el tramo no entra al grafo
 * </pre>
 */
@Component
public class CalculadoraPesos {

	static final double FACTOR_PLANO = 1.0;

	static final double FACTOR_RAMPA = 1.0;

	static final double FACTOR_PENDIENTE_FUERTE = 2.0;

	/**
	 * Devuelve el peso del tramo, o vacío si el tramo no se puede recorrer en silla de ruedas.
	 */
	public OptionalDouble peso(Condicion condicion, double metros) {
		return switch (condicion) {
			case PLANO -> OptionalDouble.of(metros * FACTOR_PLANO);
			case RAMPA -> OptionalDouble.of(metros * FACTOR_RAMPA);
			case PENDIENTE_FUERTE -> OptionalDouble.of(metros * FACTOR_PENDIENTE_FUERTE);
			case ESCALERAS -> OptionalDouble.empty();
		};
	}

}
