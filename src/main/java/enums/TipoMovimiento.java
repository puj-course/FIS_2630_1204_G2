package enums;

/**
 * El tipo de un movimiento de inventario.
 *
 * Los nombres son exactamente los dos valores que acepta la restriccion
 * ck_movimientos_tipo de la tabla movimientos_inventario. Antes eran
 * VENTA_COMANDA y REVERSO_COMANDA, que la base no acepta: con eso,
 * MovimientoInventarioRepository no podia guardar (la restriccion rechazaba la
 * fila) ni leer (valueOf reventaba con "No enum constant" en cada fila que ya
 * estaba guardada). Quien quiera saber por que se movio el inventario lo tiene
 * en la columna motivo.
 */
public enum TipoMovimiento {

    /** Entra mercancia: una compra, o la devolucion de una comanda anulada. */
    ENTRADA,

    /** Sale mercancia: el consumo de una comanda, o una merma. */
    SALIDA
}
