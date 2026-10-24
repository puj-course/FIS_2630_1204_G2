package service.alerta;

/**
 * Producto del patrón Factory Method: la alerta de stock de un ingrediente.
 *
 * Quien la usa (la pantalla de inventario) solo pide el texto y los estilos;
 * no necesita saber qué tipo de alerta es.
 */
public interface AlertaStock {

    /** Texto de la columna "Alerta". */
    String getTexto();

    /** Estilo CSS de la celda de la columna "Alerta". */
    String getEstiloCelda();

    /** Estilo CSS de toda la fila del ingrediente. */
    String getEstiloFila();
}
