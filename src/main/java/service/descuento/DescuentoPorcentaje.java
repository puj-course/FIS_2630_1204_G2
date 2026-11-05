package service.descuento;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Estrategia concreta: el cajero escribe un porcentaje y se calcula sobre el subtotal. */
public class DescuentoPorcentaje implements DescuentoStrategy {

    private static final BigDecimal CIEN = new BigDecimal("100");

    /**
     * Taller de pruebas: antes este metodo aceptaba cualquier cosa. Las unicas
     * validaciones vivian en CajeroPagoController, asi que no se podian probar
     * de forma unitaria y no protegian a ningun otro llamador. La regla ahora
     * esta donde pertenece: un porcentaje valido va de 0 a 100.
     */
    @Override
    public BigDecimal calcular(BigDecimal subtotal, BigDecimal valor) {
        if (subtotal == null || valor == null) {
            throw new IllegalArgumentException(
                    "El subtotal y el porcentaje de descuento son obligatorios");
        }
        if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El subtotal no puede ser negativo");
        }
        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El porcentaje no puede ser negativo");
        }
        if (valor.compareTo(CIEN) > 0) {
            throw new IllegalArgumentException("El porcentaje no puede superar el 100%");
        }
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
