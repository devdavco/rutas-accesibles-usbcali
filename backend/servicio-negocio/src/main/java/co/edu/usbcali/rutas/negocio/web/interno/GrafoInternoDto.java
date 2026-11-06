package co.edu.usbcali.rutas.negocio.web.interno;

import java.util.List;

/** Grafo completo del campus: todos los puntos y todos los tramos, incluidos los de escaleras. */
public record GrafoInternoDto(List<PuntoInternoDto> puntos, List<TramoInternoDto> tramos) {
}
