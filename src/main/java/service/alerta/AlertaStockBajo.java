package service.alerta;

/** Producto concreto: el stock llegó al mínimo o quedó por debajo. */
public class AlertaStockBajo implements AlertaStock {

    @Override
    public String getTexto() {
        return "STOCK BAJO";
    }

    @Override
    public String getEstiloCelda() {
        return "-fx-font-weight: bold;";
    }

    @Override
    public String getEstiloFila() {
        return "-fx-background-color: #fff3cd;";
    }
}
