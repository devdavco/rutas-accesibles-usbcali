package co.edu.usbcali.rutas.grafos.web;

import co.edu.usbcali.rutas.grafos.respuesta.GrafoVisual;
import co.edu.usbcali.rutas.grafos.respuesta.PuntoSeleccionable;
import co.edu.usbcali.rutas.grafos.respuesta.RespuestaRuta;
import co.edu.usbcali.rutas.grafos.servicio.ServicioRutas;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API pública de rutas; el gateway la expone bajo /api/rutas. */
@RestController
@RequestMapping("/api/rutas")
public class RutasController {

	private final ServicioRutas servicioRutas;

	public RutasController(ServicioRutas servicioRutas) {
		this.servicioRutas = servicioRutas;
	}

	/** Puntos que se pueden elegir como origen o destino. */
	@GetMapping("/puntos")
	public List<PuntoSeleccionable> puntos() {
		return servicioRutas.listarPuntosSeleccionables();
	}

	/** Grafo completo para el mapa (solo visualización). */
	@GetMapping("/grafo")
	public GrafoVisual grafo() {
		return servicioRutas.grafoVisual();
	}

	/** Ruta accesible de menor costo; con pasos=true incluye los pasos de Dijkstra. */
	@GetMapping
	public RespuestaRuta ruta(@RequestParam long origen, @RequestParam long destino,
			@RequestParam(defaultValue = "false") boolean pasos) {
		return servicioRutas.calcularRuta(origen, destino, pasos);
	}

}
