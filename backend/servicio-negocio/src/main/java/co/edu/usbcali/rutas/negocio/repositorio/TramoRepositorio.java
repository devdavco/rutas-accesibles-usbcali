package co.edu.usbcali.rutas.negocio.repositorio;

import co.edu.usbcali.rutas.negocio.dominio.Condicion;
import co.edu.usbcali.rutas.negocio.dominio.Tramo;
import co.edu.usbcali.rutas.negocio.web.interno.TramoInternoDto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TramoRepositorio extends JpaRepository<Tramo, Long> {

	long countByCondicion(Condicion condicion);

	/** Todos los tramos con solo los ids de sus puntos, sin cargar las entidades Punto. */
	@Query("""
			select new co.edu.usbcali.rutas.negocio.web.interno.TramoInternoDto(
			    t.id, t.puntoInicial.id, t.puntoFinal.id, t.condicion, t.longitudMetros)
			from Tramo t
			order by t.id""")
	List<TramoInternoDto> listarParaGrafo();

}
