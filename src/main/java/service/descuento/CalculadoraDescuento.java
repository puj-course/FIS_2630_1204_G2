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
        this.estrategia = exigida(estrategia);
    }

    /**
     * Taller de pruebas: aceptaba null y el fallo aparecia despues, dentro de
     * calcular(), como un NullPointerException sin contexto. Se valida aqui
     * para que el error salga en el momento en que se comete.
     */
    public void setEstrategia(DescuentoStrategy estrategia) {
        this.estrategia = exigida(estrategia);
    }

    /*
     * Estatico y privado a proposito: si el constructor llamara a setEstrategia,
     * que es sobrescribible, una subclase podria recibir la llamada antes de
     * estar construida del todo. Es la advertencia this-escape de javac.
     */
    private static DescuentoStrategy exigida(DescuentoStrategy estrategia) {
        if (estrategia == null) {
            throw new IllegalArgumentException("La estrategia de descuento es obligatoria");
        }
        return estrategia;
    }

    public DescuentoStrategy getEstrategia() {
        return estrategia;
    }

    public BigDecimal calcular(BigDecimal subtotal, BigDecimal valor) {
        return estrategia.calcular(subtotal, valor);
    }
}
