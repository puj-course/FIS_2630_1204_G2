package service.alerta;

/** Creator concreto: crea la alerta cuando no queda stock del ingrediente. */
public class AlertaAgotadoFactory extends AlertaStockFactory {

    @Override
    public AlertaStock crearAlerta() {
        return new AlertaAgotado();
    }
}
