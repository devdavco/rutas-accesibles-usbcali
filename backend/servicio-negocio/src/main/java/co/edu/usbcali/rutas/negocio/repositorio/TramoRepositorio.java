package co.edu.usbcali.rutas.negocio.repositorio;

import co.edu.usbcali.rutas.negocio.dominio.Condicion;
import co.edu.usbcali.rutas.negocio.dominio.Tramo;
import co.edu.usbcali.rutas.negocio.web.interno.TramoInternoDto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TramoRepositorio extends JpaRepository<Tramo, Long> {

	long countByCondicion(Condicion condicion);

	/** Todos los tramos con solo los ids de sus puntos, sin cargar las entidades Punto. */
	@Query("""
			select new co.edu.usbcali.rutas.negocio.web.interno.TramoInternoDto(
			    t.id, t.puntoInicial.id, t.puntoFinal.id, t.condicion, t.longitudMetros)
			from Tramo t
			order by t.id""")
	List<TramoInternoDto> listarParaGrafo();

	/** Si ya hay un tramo entre los dos puntos, en cualquier orden (el grafo es no dirigido). */
	@Query("""
			select count(t) > 0 from Tramo t
			where (t.puntoInicial.id = :a and t.puntoFinal.id = :b) or (t.puntoInicial.id = :b and t.puntoFinal.id = :a)""")
	boolean existeEntre(@Param("a") Long a, @Param("b") Long b);

	@Query("select count(t) from Tramo t where t.puntoInicial.id = :puntoId or t.puntoFinal.id = :puntoId")
	long contarDelPunto(@Param("puntoId") Long puntoId);

}
