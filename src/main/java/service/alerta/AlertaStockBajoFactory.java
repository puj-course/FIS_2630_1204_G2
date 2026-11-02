package service.alerta;

/** Creator concreto: crea la alerta cuando el stock llegó al mínimo o quedó por debajo. */
public class AlertaStockBajoFactory extends AlertaStockFactory {

    @Override
    public AlertaStock crearAlerta() {
        return new AlertaStockBajo();
    }
}
