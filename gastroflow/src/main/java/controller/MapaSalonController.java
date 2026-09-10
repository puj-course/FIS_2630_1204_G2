package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import entity.Mesa;
import repository.MesaRepository;
import javafx.scene.layout.Pane;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class MapaSalonController {

    @FXML
    private Pane panelMesas;

    private final MesaRepository mesaRepository = new MesaRepository();

    private Mesa mesaSeleccionada;

    @FXML
    public void initialize() {
        cargarMesas();
    }

    private void cargarMesas() {
        try {
            List<Mesa> mesas = mesaRepository.obtenerTodas();

            for (Mesa mesa : mesas) {

                // Solo agrega la mesa si todavía no está en pantalla
                boolean existe = panelMesas.getChildren().stream()
                        .anyMatch(n -> n.getId() != null &&
                                n.getId().equals("mesa-" + mesa.getIdMesa()));

                if (!existe) {
                    Button boton = crearBotonMesa(mesa);
                    boton.setId("mesa-" + mesa.getIdMesa());
                    panelMesas.getChildren().add(boton);
                }
            }

        } catch (SQLException e) {
            mostrarError("Error al cargar mesas: " + e.getMessage());
        }
    }

    private Button crearBotonMesa(Mesa mesa) {
        Button btn = new Button(
                "Mesa " + mesa.getNumeroMesa() + "\n" +
                        mesa.getNombreZona()
        );

        btn.setPrefSize(100, 80);

        btn.setLayoutX(50 + (mesa.getNumeroMesa() - 1) * 120);
        btn.setLayoutY(50);

        btn.setStyle(
                "-fx-background-color: " +
                        colorSegunEstado(mesa.getCodigoEstado()) +
                        "; -fx-text-fill: white; -fx-font-weight: bold;"
        );

        btn.setOnAction(e -> {
            mesaSeleccionada = mesa;
            mostrarDetalleMesa(mesa);
        });

        return btn;
    }

    private String colorSegunEstado(String codigoEstado) {
        return switch (codigoEstado) {
            case "LIBRE" -> "#4CAF50";
            case "OCUPADA" -> "#F44336";
            case "RESERVADA" -> "#FF9800";
            case "MANTENIMIENTO" -> "#9E9E9E";
            default -> "#2196F3";
        };
    }

    @FXML
    private void agregarMesa() {
        TextInputDialog dialog = new TextInputDialog();

        dialog.setTitle("Agregar mesa");
        dialog.setHeaderText("Nueva mesa");
        dialog.setContentText("Número de mesa:");

        Optional<String> resultado = dialog.showAndWait();

        if (resultado.isPresent()) {
            try {
                int numeroMesa = Integer.parseInt(resultado.get());

                mesaRepository.agregarMesa(numeroMesa);

                cargarMesas();

            } catch (NumberFormatException e) {
                mostrarError("Ingrese un número válido.");
            } catch (SQLException e) {
                mostrarError("Error al agregar mesa: " + e.getMessage());
            }
        }
    }

    @FXML
    private void quitarMesa() {
        if (mesaSeleccionada == null) {
            mostrarError("Seleccione una mesa primero.");
            return;
        }

        try {
            mesaRepository.quitarMesa(mesaSeleccionada.getIdMesa());

            // Buscar y eliminar solamente el botón de esa mesa
            String idBoton = "mesa-" + mesaSeleccionada.getIdMesa();

            panelMesas.getChildren().removeIf(
                    nodo -> idBoton.equals(nodo.getId())
            );

            mesaSeleccionada = null;

        } catch (SQLException e) {
            mostrarError("Error al quitar mesa: " + e.getMessage());
        }
    }

    private void mostrarDetalleMesa(Mesa mesa) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Detalle de Mesa");
        alert.setHeaderText("Mesa " + mesa.getNumeroMesa());

        alert.setContentText(
                "Zona: " + mesa.getNombreZona() + "\n" +
                        "Capacidad: " + mesa.getCapacidad() + " personas\n" +
                        "Estado: " + mesa.getCodigoEstado() + "\n" +
                        "Código: " +
                        (mesa.getCodigoMesa() != null ?
                                mesa.getCodigoMesa() : "N/A")
        );

        alert.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
//sigue en desarrollo esto para que las mesas se puedan colocar bien