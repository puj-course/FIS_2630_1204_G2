package service.descuento;

import java.math.BigDecimal;

/**
 * Patrón Strategy: define una familia de algoritmos intercambiables.
 *
 * Cada implementación sabe convertir el valor que escribe el cajero en el
 * descuento real, en pesos, que se resta de la cuenta.
 */
public interface DescuentoStrategy {

    /**
     * @param subtotal subtotal de la cuenta, en pesos
     * @param valor    lo que escribió el cajero (pesos o porcentaje, según la estrategia)
     * @return el descuento en pesos, con dos decimales
     */
    BigDecimal calcular(BigDecimal subtotal, BigDecimal valor);

    /** Nombre que se muestra en la pantalla de pago. */
    String getNombre();
}
