package co.edu.usbcali.rutas.negocio.repositorio;

import co.edu.usbcali.rutas.negocio.dominio.Administrador;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministradorRepositorio extends JpaRepository<Administrador, Long> {

	Optional<Administrador> findByCorreo(String correo);

}
