package service.descuento;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Estrategia concreta: el cajero escribe directamente cuántos pesos se descuentan. */
public class DescuentoMontoFijo implements DescuentoStrategy {

    @Override
    public BigDecimal calcular(BigDecimal subtotal, BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getNombre() {
        return "Monto fijo ($)";
    }

    @Override
    public String toString() {
        return getNombre();
    }
}
