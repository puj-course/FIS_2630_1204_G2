package enums;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Patrón Strategy: cada constante es una forma distinta de calcular el
 * descuento. Todas responden al mismo método, calcular(), y el cajero elige
 * cuál usar en la pantalla de pago.
 *
 * Para agregar otro tipo de descuento basta con añadir una constante más.
 */
public enum TipoDescuento {

    /** Lo que escribe el cajero son los pesos a descontar. */
    MONTO_FIJO("Monto fijo ($)") {
        @Override
        public BigDecimal calcular(BigDecimal subtotal, BigDecimal valor) {
            return valor.setScale(2, RoundingMode.HALF_UP);
        }
    },

    /** Lo que escribe el cajero es un porcentaje del subtotal. */
    PORCENTAJE("Porcentaje (%)") {
        @Override
        public BigDecimal calcular(BigDecimal subtotal, BigDecimal valor) {
            return subtotal.multiply(valor)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
    };

    private final String nombre;

    TipoDescuento(String nombre) {
        this.nombre = nombre;
    }

    /**
     * @param subtotal subtotal de la cuenta, en pesos
     * @param valor    lo que escribió el cajero (pesos o porcentaje)
     * @return el descuento en pesos, con dos decimales
     */
    public abstract BigDecimal calcular(BigDecimal subtotal, BigDecimal valor);

    /** Es el texto que muestra el desplegable. */
    @Override
    public String toString() {
        return nombre;
    }
}
