package co.edu.usbcali.rutas.negocio.web.interno;

import co.edu.usbcali.rutas.negocio.repositorio.PuntoRepositorio;
import co.edu.usbcali.rutas.negocio.repositorio.TramoRepositorio;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lectura del grafo para servicio-grafos (tráfico entre servicios).
 * No pasa por el gateway: el gateway solo enruta /api/admin/** a este servicio.
 */
@RestController
@RequestMapping("/interno")
public class GrafoInternoController {

	private final PuntoRepositorio puntoRepositorio;

	private final TramoRepositorio tramoRepositorio;

	public GrafoInternoController(PuntoRepositorio puntoRepositorio, TramoRepositorio tramoRepositorio) {
		this.puntoRepositorio = puntoRepositorio;
		this.tramoRepositorio = tramoRepositorio;
	}

	@GetMapping("/grafo")
	@Transactional(readOnly = true)
	public GrafoInternoDto grafo() {
		var puntos = puntoRepositorio.findAll(Sort.by("id")).stream().map(PuntoInternoDto::desde).toList();
		return new GrafoInternoDto(puntos, tramoRepositorio.listarParaGrafo());
	}

}
