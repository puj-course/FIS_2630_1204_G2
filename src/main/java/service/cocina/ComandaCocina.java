package service.cocina;

import entity.Pedido;

/**
 * Interfaz Target del patrón Adapter: lo que el tablero de cocina espera de
 * una comanda. La pantalla habla en comandas (columna, minutos de espera,
 * acción) y no en pedidos (estados, fechas, ids).
 */
public interface ComandaCocina {

    /** Columna del tablero donde se muestra la comanda. */
    enum Columna {
        PENDIENTES,
        PREPARACION,
        LISTOS
    }

    String getNumero();

    Long getMesaId();

    long getMinutosEspera();

    Columna getColumna();

    /** false cuando la comanda ya está lista y no tiene siguiente paso en cocina. */
    boolean puedeAvanzar();

    /** Texto del botón de la tarjeta, por ejemplo "Pasar a preparación". */
    String getTextoAccion();

    /** Pasa la comanda al siguiente paso y devuelve el pedido ya actualizado. */
    Pedido avanzar();
}
