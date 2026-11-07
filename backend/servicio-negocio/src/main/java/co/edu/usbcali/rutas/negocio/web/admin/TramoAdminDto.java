package co.edu.usbcali.rutas.negocio.web.admin;

import co.edu.usbcali.rutas.negocio.dominio.Condicion;
import co.edu.usbcali.rutas.negocio.dominio.Tramo;
import co.edu.usbcali.rutas.negocio.web.interno.TramoInternoDto;

/** Tramo tal como lo ve el administrador; la longitud la calcula PostGIS. */
public record TramoAdminDto(Long id, Long puntoInicialId, Long puntoFinalId, Condicion condicion, Double longitudM) {

	public static TramoAdminDto desde(Tramo tramo) {
		return new TramoAdminDto(tramo.getId(), tramo.getPuntoInicial().getId(), tramo.getPuntoFinal().getId(),
				tramo.getCondicion(), tramo.getLongitudMetros());
	}

	public static TramoAdminDto desde(TramoInternoDto tramo) {
		return new TramoAdminDto(tramo.id(), tramo.puntoInicialId(), tramo.puntoFinalId(), tramo.condicion(),
				tramo.longitudM());
	}

}
