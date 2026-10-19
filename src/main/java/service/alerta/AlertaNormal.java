package service.alerta;

/** Producto concreto: el stock está por encima del mínimo. */
public class AlertaNormal implements AlertaStock {

    @Override
    public String getTexto() {
        return "NORMAL";
    }

    @Override
    public String getEstiloCelda() {
        return "";
    }

    @Override
    public String getEstiloFila() {
        return "";
    }
}
