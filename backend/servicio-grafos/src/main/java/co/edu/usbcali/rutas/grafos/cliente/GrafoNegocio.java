package co.edu.usbcali.rutas.grafos.cliente;

import java.util.List;

/** Respuesta de /interno/grafo: todos los puntos y tramos, incluidos los de escaleras. */
public record GrafoNegocio(List<PuntoNegocio> puntos, List<TramoNegocio> tramos) {
}
