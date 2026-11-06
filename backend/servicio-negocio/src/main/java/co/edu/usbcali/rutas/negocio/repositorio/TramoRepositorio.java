package co.edu.usbcali.rutas.negocio.repositorio;

import co.edu.usbcali.rutas.negocio.dominio.Condicion;
import co.edu.usbcali.rutas.negocio.dominio.Tramo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TramoRepositorio extends JpaRepository<Tramo, Long> {

	long countByCondicion(Condicion condicion);

}
