package co.edu.usbcali.rutas.grafos.respuesta;

import co.edu.usbcali.rutas.grafos.dominio.Condicion;

/**
 * Tramos consecutivos de la ruta con la misma condición, unidos para la descripción textual
 * (RF05). Por ejemplo: "120 m en plano, luego 15 m de rampa".
 */
public record Segmento(Condicion condicion, double metros, long desdeId, String desde, long hastaId, String hasta) {
}
