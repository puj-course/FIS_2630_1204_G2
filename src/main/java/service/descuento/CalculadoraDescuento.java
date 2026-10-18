package service.descuento;

import java.math.BigDecimal;

/**
 * Contexto del patrón Strategy.
 *
 * No sabe cómo se calcula un descuento: delega en la estrategia que tenga
 * asignada, y esa estrategia se puede cambiar en tiempo de ejecución.
 */
public class CalculadoraDescuento {

    private DescuentoStrategy estrategia;

    public CalculadoraDescuento() {
        this(new DescuentoMontoFijo());
    }

    public CalculadoraDescuento(DescuentoStrategy estrategia) {
        this.estrategia = estrategia;
    }

    public void setEstrategia(DescuentoStrategy estrategia) {
        this.estrategia = estrategia;
    }

    public DescuentoStrategy getEstrategia() {
        return estrategia;
    }

    public BigDecimal calcular(BigDecimal subtotal, BigDecimal valor) {
        return estrategia.calcular(subtotal, valor);
    }
}
