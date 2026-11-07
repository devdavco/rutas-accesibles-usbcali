package co.edu.usbcali.rutas.negocio.web.admin;

import co.edu.usbcali.rutas.negocio.dominio.Condicion;
import co.edu.usbcali.rutas.negocio.dominio.TipoPunto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cuerpos de las peticiones del administrador, con sus validaciones y mensajes en español. */
public final class Solicitudes {

	private Solicitudes() {
	}

	public record Ingreso(
			@NotBlank(message = "El correo es obligatorio.") String correo,
			@NotBlank(message = "La contraseña es obligatoria.") String contrasena) {
	}

	public record Punto(
			@NotBlank(message = "El nombre es obligatorio.")
			@Size(max = 120, message = "El nombre admite máximo 120 caracteres.") String nombre,
			@NotNull(message = "El tipo es obligatorio.") TipoPunto tipo,
			@Size(max = 60, message = "El edificio admite máximo 60 caracteres.") String edificio,
			boolean seleccionable,
			@NotNull(message = "La latitud es obligatoria.")
			@DecimalMin(value = "-90", message = "La latitud debe estar entre -90 y 90.")
			@DecimalMax(value = "90", message = "La latitud debe estar entre -90 y 90.") Double lat,
			@NotNull(message = "La longitud es obligatoria.")
			@DecimalMin(value = "-180", message = "La longitud debe estar entre -180 y 180.")
			@DecimalMax(value = "180", message = "La longitud debe estar entre -180 y 180.") Double lng) {
	}

	public record NuevoTramo(
			@NotNull(message = "El punto inicial es obligatorio.") Long puntoInicialId,
			@NotNull(message = "El punto final es obligatorio.") Long puntoFinalId,
			@NotNull(message = "La condición es obligatoria.") Condicion condicion) {
	}

	public record CambioCondicion(@NotNull(message = "La condición es obligatoria.") Condicion condicion) {
	}

}
