package service.alerta;

/** Creator concreto: crea la alerta cuando el stock está por encima del mínimo. */
public class AlertaNormalFactory extends AlertaStockFactory {

    @Override
    public AlertaStock crearAlerta() {
        return new AlertaNormal();
    }
}
