package co.edu.usbcali.rutas.grafos.servicio;

import co.edu.usbcali.rutas.grafos.algoritmo.Dijkstra;
import co.edu.usbcali.rutas.grafos.algoritmo.ResultadoDijkstra;
import co.edu.usbcali.rutas.grafos.cliente.ClienteNegocio;
import co.edu.usbcali.rutas.grafos.cliente.GrafoNegocio;
import co.edu.usbcali.rutas.grafos.dominio.CalculadoraPesos;
import co.edu.usbcali.rutas.grafos.dominio.Condicion;
import co.edu.usbcali.rutas.grafos.dominio.Grafo;
import co.edu.usbcali.rutas.grafos.dominio.Punto;
import co.edu.usbcali.rutas.grafos.respuesta.GrafoVisual;
import co.edu.usbcali.rutas.grafos.respuesta.PuntoSeleccionable;
import co.edu.usbcali.rutas.grafos.respuesta.RespuestaRuta;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Cálculo de rutas accesibles: carga el grafo, valida la solicitud y ejecuta Dijkstra. */
@Service
public class ServicioRutas {

	/** Orden alfabético en español: ignora mayúsculas y ubica bien las tildes. */
	private static final Collator ESPANOL = Collator.getInstance(Locale.forLanguageTag("es-CO"));

	/** Entradas del campus (sin edificio) primero; luego por edificio y nombre. */
	private static final Comparator<Punto> POR_EDIFICIO_Y_NOMBRE = Comparator
		.comparing(Punto::edificio, Comparator.nullsFirst(ESPANOL::compare))
		.thenComparing(Punto::nombre, ESPANOL::compare);

	private final ClienteNegocio clienteNegocio;

	private final CalculadoraPesos calculadoraPesos;

	private final Dijkstra dijkstra;

	private final ConstructorRespuesta constructorRespuesta;

	public ServicioRutas(ClienteNegocio clienteNegocio, CalculadoraPesos calculadoraPesos, Dijkstra dijkstra,
			ConstructorRespuesta constructorRespuesta) {
		this.clienteNegocio = clienteNegocio;
		this.calculadoraPesos = calculadoraPesos;
		this.dijkstra = dijkstra;
		this.constructorRespuesta = constructorRespuesta;
	}

	public List<PuntoSeleccionable> listarPuntosSeleccionables() {
		return cargarGrafo().puntos()
			.stream()
			.filter(Punto::seleccionable)
			.sorted(POR_EDIFICIO_Y_NOMBRE)
			.map(p -> new PuntoSeleccionable(p.id(), p.nombre(), p.tipo(), p.edificio()))
			.toList();
	}

	/** Todos los puntos y tramos, tal como los entrega servicio-negocio, para dibujarlos. */
	public GrafoVisual grafoVisual() {
		GrafoNegocio datos = clienteNegocio.obtenerGrafo();
		return new GrafoVisual(
				datos.puntos()
					.stream()
					.map(p -> new GrafoVisual.PuntoVisual(p.id(), p.nombre(), p.tipo(), p.edificio(), p.seleccionable(),
							p.lat(), p.lng()))
					.toList(),
				datos.tramos()
					.stream()
					.map(t -> new GrafoVisual.TramoVisual(t.id(), t.puntoInicialId(), t.puntoFinalId(), t.condicion(),
							t.longitudM(), pesoODesconocido(t.condicion(), t.longitudM())))
					.toList());
	}

	/** Peso del tramo con la misma regla que usa Dijkstra; nulo si no se puede recorrer. */
	private Double pesoODesconocido(Condicion condicion, double metros) {
		var peso = calculadoraPesos.peso(condicion, metros);
		return peso.isPresent() ? peso.getAsDouble() : null;
	}

	public RespuestaRuta calcularRuta(long origen, long destino, boolean incluirPasos) {
		// El grafo se carga en cada cálculo (RF10): así se usan siempre los datos vigentes.
		Grafo grafo = cargarGrafo();
		validarSeleccionable(grafo, origen, "origen");
		validarSeleccionable(grafo, destino, "destino");

		ResultadoDijkstra resultado = dijkstra.calcular(grafo, origen, destino, incluirPasos);
		return constructorRespuesta.construir(grafo, origen, destino, resultado, incluirPasos);
	}

	private Grafo cargarGrafo() {
		GrafoNegocio datos = clienteNegocio.obtenerGrafo();
		Grafo.Constructor constructor = Grafo.constructor(calculadoraPesos);
		datos.puntos()
			.forEach(p -> constructor.punto(
					new Punto(p.id(), p.nombre(), p.tipo(), p.edificio(), p.seleccionable(), p.lat(), p.lng())));
		datos.tramos()
			.forEach(t -> constructor.tramo(t.id(), t.puntoInicialId(), t.puntoFinalId(), t.condicion(),
					t.longitudM()));
		return constructor.construir();
	}

	private static void validarSeleccionable(Grafo grafo, long puntoId, String rol) {
		if (!grafo.contiene(puntoId)) {
			throw new SolicitudRutaInvalidaException("El punto de %s %d no existe.".formatted(rol, puntoId));
		}
		Punto punto = grafo.punto(puntoId);
		if (!punto.seleccionable()) {
			throw new SolicitudRutaInvalidaException(
					"El punto de %s %d (%s) no se puede elegir como origen o destino.".formatted(rol, puntoId,
							punto.nombre()));
		}
	}

}
