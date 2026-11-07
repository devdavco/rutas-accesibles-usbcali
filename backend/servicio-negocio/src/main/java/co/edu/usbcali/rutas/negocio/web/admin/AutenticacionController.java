package co.edu.usbcali.rutas.negocio.web.admin;

import co.edu.usbcali.rutas.negocio.seguridad.ServicioAutenticacion;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ingreso del administrador. No hay registro: el único usuario se crea al arrancar. */
@RestController
@RequestMapping("/api/admin/autenticacion")
public class AutenticacionController {

	public record RespuestaIngreso(String token, Instant expiraEn) {
	}

	private final ServicioAutenticacion autenticacion;

	public AutenticacionController(ServicioAutenticacion autenticacion) {
		this.autenticacion = autenticacion;
	}

	@PostMapping("/ingresar")
	public RespuestaIngreso ingresar(@Valid @RequestBody Solicitudes.Ingreso datos) {
		ServicioAutenticacion.Token token = autenticacion.ingresar(datos.correo(), datos.contrasena());
		return new RespuestaIngreso(token.valor(), token.expiraEn());
	}

}
