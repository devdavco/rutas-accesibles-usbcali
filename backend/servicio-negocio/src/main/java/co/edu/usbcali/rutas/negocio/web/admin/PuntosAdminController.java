package co.edu.usbcali.rutas.negocio.web.admin;

import co.edu.usbcali.rutas.negocio.servicio.ServicioGrafoAdmin;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Edición de puntos (requiere el JWT del administrador). */
@RestController
@RequestMapping("/api/admin/puntos")
public class PuntosAdminController {

	private final ServicioGrafoAdmin servicio;

	public PuntosAdminController(ServicioGrafoAdmin servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	public List<PuntoAdminDto> listar() {
		return servicio.listarPuntos();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PuntoAdminDto crear(@Valid @RequestBody Solicitudes.Punto datos) {
		return servicio.crearPunto(datos);
	}

	@PutMapping("/{id}")
	public PuntoAdminDto editar(@PathVariable Long id, @Valid @RequestBody Solicitudes.Punto datos) {
		return servicio.editarPunto(id, datos);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminar(@PathVariable Long id) {
		servicio.eliminarPunto(id);
	}

}
