package controller;

import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Búsqueda rápida de mesa por número, con resalte visual sobre el mapa.
 * No consulta la base de datos: opera sobre los botones ya renderizados
 * en el Pane del salón (panelMesas), así que responde de inmediato.
 */
public class BusquedaMesaHelper {

    private static final Pattern NUMERO_EN_TEXTO = Pattern.compile("\\d+");

    private static final String ESTILO_RESALTADO =
            "-fx-border-color: gold; -fx-border-width: 4; -fx-effect: dropshadow(gaussian, gold, 20, 0.6, 0, 0);";

    // Llamar desde el manejador del input de búsqueda (onAction o listener del TextField)
    public static void buscarYResaltarMesa(Pane panelMesas, String numeroMesaTexto) {

        if (numeroMesaTexto == null || numeroMesaTexto.isBlank()) {
            return;
        }

        Button botonEncontrado =
                panelMesas.getChildren().stream()
                        .filter(nodo -> nodo instanceof Button)
                        .map(nodo -> (Button) nodo)
                        .filter(boton -> coincideNumeroMesa(boton, numeroMesaTexto.trim()))
                        .findFirst()
                        .orElse(null);

        if (botonEncontrado == null) {
            mostrarMesaNoEncontrada(numeroMesaTexto);
            return;
        }

        resaltarBoton(botonEncontrado);
    }

    private static boolean coincideNumeroMesa(Button boton, String numeroBuscado) {
        if (boton.getText() == null) {
            return false;
        }

        Matcher matcher = NUMERO_EN_TEXTO.matcher(boton.getText());

        return matcher.find() && matcher.group().equals(numeroBuscado);
    }

    private static void resaltarBoton(Button boton) {
        String estiloOriginal = boton.getStyle();

        boton.setStyle(estiloOriginal + ESTILO_RESALTADO);

        ScaleTransition pulso = new ScaleTransition(Duration.millis(250), boton);
        pulso.setFromX(1.0);
        pulso.setFromY(1.0);
        pulso.setToX(1.3);
        pulso.setToY(1.3);
        pulso.setAutoReverse(true);
        pulso.setCycleCount(2);
        pulso.play();

        PauseTransition quitarResaltado = new PauseTransition(Duration.seconds(2.5));
        quitarResaltado.setOnFinished(evento -> boton.setStyle(estiloOriginal));
        quitarResaltado.play();
    }

    private static void mostrarMesaNoEncontrada(String numeroBuscado) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Búsqueda de mesa");
        alert.setHeaderText(null);
        alert.setContentText("Mesa no encontrada: " + numeroBuscado);
        alert.showAndWait();
    }
}
