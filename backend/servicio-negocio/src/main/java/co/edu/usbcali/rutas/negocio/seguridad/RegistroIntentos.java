package co.edu.usbcali.rutas.negocio.seguridad;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Bloqueo temporal tras varios ingresos fallidos seguidos para un mismo correo (en memoria).
 * Se aplica a cualquier correo, exista o no, para no revelar cuáles existen.
 * Limitaciones aceptadas (ver docs/seguridad.md): se pierde al reiniciar, no se comparte entre
 * instancias y un tercero puede bloquear temporalmente al administrador.
 */
@Component
public class RegistroIntentos {

	static final int MAXIMO_FALLOS = 5;

	static final Duration DURACION_BLOQUEO = Duration.ofMinutes(5);

	private record Estado(int fallos, Instant bloqueadoHasta) {
	}

	private final Map<String, Estado> estados = new ConcurrentHashMap<>();

	private final Clock reloj;

	public RegistroIntentos(Clock reloj) {
		this.reloj = reloj;
	}

	public boolean estaBloqueado(String correo) {
		Estado estado = estados.get(clave(correo));
		return estado != null && estado.bloqueadoHasta() != null && reloj.instant().isBefore(estado.bloqueadoHasta());
	}

	public void registrarFallo(String correo) {
		estados.compute(clave(correo), (c, anterior) -> {
			// Un bloqueo vencido empieza de cero
			int fallos = (anterior == null || anterior.bloqueadoHasta() != null) ? 1 : anterior.fallos() + 1;
			Instant bloqueo = fallos >= MAXIMO_FALLOS ? reloj.instant().plus(DURACION_BLOQUEO) : null;
			return new Estado(fallos, bloqueo);
		});
	}

	public void registrarExito(String correo) {
		estados.remove(clave(correo));
	}

	/** Olvida todos los intentos (lo usan las pruebas). */
	public void reiniciar() {
		estados.clear();
	}

	private static String clave(String correo) {
		return correo.trim().toLowerCase(Locale.ROOT);
	}

}
