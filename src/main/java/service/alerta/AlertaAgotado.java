package service.alerta;

/** ConcreteProduct: no queda stock del ingrediente. */
public class AlertaAgotado implements AlertaStock {

    @Override
    public String getTexto() {
        return "AGOTADO";
    }

    @Override
    public String getEstiloCelda() {
        return "-fx-font-weight: bold; -fx-text-fill: #b02a37;";
    }

    @Override
    public String getEstiloFila() {
        return "-fx-background-color: #f8d7da;";
    }
}
