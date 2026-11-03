package service.descuento;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Estrategia concreta: el cajero escribe directamente cuántos pesos se descuentan. */
public class DescuentoMontoFijo implements DescuentoStrategy {

    /**
     * Taller de pruebas: esta estrategia ignoraba por completo el subtotal, de
     * modo que un descuento de $90.000 sobre una cuenta de $20.000 devolvia
     * $90.000. En la pantalla de caja eso lo frenaba un if del controlador;
     * el servicio en si no tenia contrato. Ahora lo tiene.
     */
    @Override
    public BigDecimal calcular(BigDecimal subtotal, BigDecimal valor) {
        if (subtotal == null || valor == null) {
            throw new IllegalArgumentException(
                    "El subtotal y el monto del descuento son obligatorios");
        }
        if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El subtotal no puede ser negativo");
        }
        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El descuento no puede ser negativo");
        }
        if (valor.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException(
                    "El descuento no puede superar el subtotal de la cuenta");
        }
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
