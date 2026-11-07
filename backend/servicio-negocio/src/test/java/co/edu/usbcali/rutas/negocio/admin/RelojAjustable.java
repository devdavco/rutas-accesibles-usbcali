package co.edu.usbcali.rutas.negocio.admin;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Reloj de pruebas que se puede adelantar para simular el paso del tiempo. */
public class RelojAjustable extends Clock {

	private Instant ahora = Instant.now();

	public void adelantar(Duration tiempo) {
		ahora = ahora.plus(tiempo);
	}

	public void reiniciar() {
		ahora = Instant.now();
	}

	@Override
	public Instant instant() {
		return ahora;
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zona) {
		return this;
	}

}
