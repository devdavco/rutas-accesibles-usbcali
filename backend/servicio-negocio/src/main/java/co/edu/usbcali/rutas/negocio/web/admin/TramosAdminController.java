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

/** Edición de tramos (requiere el JWT del administrador). */
@RestController
@RequestMapping("/api/admin/tramos")
public class TramosAdminController {

	private final ServicioGrafoAdmin servicio;

	public TramosAdminController(ServicioGrafoAdmin servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	public List<TramoAdminDto> listar() {
		return servicio.listarTramos();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TramoAdminDto crear(@Valid @RequestBody Solicitudes.NuevoTramo datos) {
		return servicio.crearTramo(datos);
	}

	/** Solo cambia la condición: para cambiar los extremos se elimina y se crea otro tramo. */
	@PutMapping("/{id}")
	public TramoAdminDto cambiarCondicion(@PathVariable Long id, @Valid @RequestBody Solicitudes.CambioCondicion datos) {
		return servicio.cambiarCondicion(id, datos);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminar(@PathVariable Long id) {
		servicio.eliminarTramo(id);
	}

}
