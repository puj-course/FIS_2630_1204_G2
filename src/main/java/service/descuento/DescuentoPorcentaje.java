package service.descuento;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Estrategia concreta: el cajero escribe un porcentaje y se calcula sobre el subtotal. */
public class DescuentoPorcentaje implements DescuentoStrategy {

    private static final BigDecimal CIEN = new BigDecimal("100");

    @Override
    public BigDecimal calcular(BigDecimal subtotal, BigDecimal valor) {
        return subtotal.multiply(valor).divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    @Override
    public String getNombre() {
        return "Porcentaje (%)";
    }

    @Override
    public String toString() {
        return getNombre();
    }
}
