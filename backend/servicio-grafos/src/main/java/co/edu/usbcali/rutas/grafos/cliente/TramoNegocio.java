package co.edu.usbcali.rutas.grafos.cliente;

import co.edu.usbcali.rutas.grafos.dominio.Condicion;

/** Tramo tal como lo entrega servicio-negocio en /interno/grafo. */
public record TramoNegocio(long id, long puntoInicialId, long puntoFinalId, Condicion condicion, double longitudM) {
}
