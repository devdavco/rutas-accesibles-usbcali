package co.edu.usbcali.rutas.negocio.web.interno;

import co.edu.usbcali.rutas.negocio.dominio.Condicion;

/** Tramo del grafo tal como lo recibe servicio-grafos. */
public record TramoInternoDto(Long id, Long puntoInicialId, Long puntoFinalId, Condicion condicion, Double longitudM) {
}
