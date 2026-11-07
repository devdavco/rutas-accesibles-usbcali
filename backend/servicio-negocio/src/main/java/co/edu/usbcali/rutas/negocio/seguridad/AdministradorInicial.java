package co.edu.usbcali.rutas.negocio.seguridad;

import co.edu.usbcali.rutas.negocio.dominio.Administrador;
import co.edu.usbcali.rutas.negocio.repositorio.AdministradorRepositorio;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el administrador inicial al arrancar, con ADMIN_CORREO y ADMIN_CONTRASENA, si todavía no
 * existe. La contraseña se guarda cifrada con BCrypt. No hay endpoint de registro.
 */
@Component
public class AdministradorInicial implements ApplicationRunner {

	private static final Logger LOG = LoggerFactory.getLogger(AdministradorInicial.class);

	private final AdministradorRepositorio administradores;

	private final PasswordEncoder codificador;

	private final String correo;

	private final String contrasena;

	public AdministradorInicial(AdministradorRepositorio administradores, PasswordEncoder codificador,
			@Value("${rutas.admin.correo}") String correo, @Value("${rutas.admin.contrasena}") String contrasena) {
		this.administradores = administradores;
		this.codificador = codificador;
		this.correo = correo.trim().toLowerCase(Locale.ROOT);
		this.contrasena = contrasena;
	}

	@Override
	public void run(ApplicationArguments argumentos) {
		if (correo.isBlank() || contrasena.isBlank()) {
			LOG.warn("ADMIN_CORREO o ADMIN_CONTRASENA no están definidos: no se crea el administrador inicial");
			return;
		}
		if (administradores.findByCorreo(correo).isPresent()) {
			return;
		}
		administradores.save(new Administrador(correo, codificador.encode(contrasena)));
		LOG.info("Administrador inicial creado: {}", correo);
	}

}
