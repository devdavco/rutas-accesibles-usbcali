package co.edu.usbcali.rutas.negocio.repositorio;

import co.edu.usbcali.rutas.negocio.dominio.Punto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PuntoRepositorio extends JpaRepository<Punto, Long> {

	/** Puntos que se pueden elegir como origen o destino. */
	List<Punto> findBySeleccionableTrue();

}
