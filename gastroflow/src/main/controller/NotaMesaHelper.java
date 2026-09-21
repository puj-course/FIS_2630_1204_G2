package controller;

import entity.NotaMesa;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import repository.NotaMesaRepository;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Métodos de apoyo para agregar la nota rápida de mesa sobre el mapa del salón.
 * Se llaman desde MapaSalonController sin necesidad de reescribirlo.
 */
public class NotaMesaHelper {

    private static final NotaMesaRepository notaMesaRepository = new NotaMesaRepository();

    // Llamar desde crearBotonMesa(), después de crear cada botón de mesa,
    // para que el badge aparezca si esa mesa ya tiene una nota activa.
    public static void aplicarBadgeSiTieneNota(Button botonMesa, int idMesa) {
        try {
            Optional<NotaMesa> nota = notaMesaRepository.obtenerNotaPorMesa(idMesa);

            if (nota.isPresent()) {
                Label badge = new Label("📝");
                badge.setStyle("-fx-font-size: 14px;");
                botonMesa.setGraphic(badge);
                botonMesa.setContentDisplay(javafx.scene.control.ContentDisplay.TOP);
                botonMesa.setAlignment(Pos.CENTER);
            } else {
                botonMesa.setGraphic(null);
            }

        } catch (SQLException e) {
            // Si falla la consulta de la nota, se deja el botón sin badge en vez de romper el mapa
            botonMesa.setGraphic(null);
        }
    }

    // Llamar desde mostrarDetalleMesa() (o un botón nuevo "Nota") para agregar/editar la nota
    public static void mostrarDialogoNota(int idMesa, int numeroMesa, Runnable alTerminar) {
        try {
            Optional<NotaMesa> notaActual = notaMesaRepository.obtenerNotaPorMesa(idMesa);
            String textoActual = notaActual.map(NotaMesa::getTextoNota).orElse("");

            TextInputDialog dialog = new TextInputDialog(textoActual);
            dialog.setTitle("Nota de la mesa " + numeroMesa);
            dialog.setHeaderText("Nota rápida para la mesa " + numeroMesa);
            dialog.setContentText("Ej: cumpleaños, reserva VIP, silla alta requerida");

            Optional<String> resultado = dialog.showAndWait();

            resultado.ifPresent(texto -> {
                try {
                    if (texto.isBlank()) {
                        notaMesaRepository.eliminarNota(idMesa);
                    } else {
                        notaMesaRepository.guardarNota(idMesa, texto.trim());
                    }
                    if (alTerminar != null) {
                        alTerminar.run();
                    }
                } catch (SQLException e) {
                    mostrarError("Error al guardar la nota: " + e.getMessage());
                }
            });

        } catch (SQLException e) {
            mostrarError("Error al consultar la nota: " + e.getMessage());
        }
    }

    // Llamar desde un botón "Eliminar nota" en el detalle de la mesa
    public static void eliminarNota(int idMesa, Runnable alTerminar) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setContentText("¿Eliminar la nota de esta mesa?");

        Optional<ButtonType> respuesta = confirmacion.showAndWait();

        if (respuesta.isPresent() && respuesta.get() == ButtonType.OK) {
            try {
                notaMesaRepository.eliminarNota(idMesa);
                if (alTerminar != null) {
                    alTerminar.run();
                }
            } catch (SQLException e) {
                mostrarError("Error al eliminar la nota: " + e.getMessage());
            }
        }
    }

    private static void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
