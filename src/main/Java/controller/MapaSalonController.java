package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Pane;
import entity.Mesa;
import repository.MesaRepository;
import javafx.scene.control.TextInputDialog;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MapaSalonController {

    @FXML
    private Pane panelMesas;

    @FXML
    private Label lblMesasLibres;

    @FXML
    private Label lblMesasOcupadas;

    @FXML
    private Label lblMesasReservadas;

    @FXML
    private TextField txtBuscarMesa;

    private final MesaRepository mesaRepository = new MesaRepository();

    private Mesa mesaSeleccionada;

    private List<Mesa> mesasActuales = new ArrayList<>();

    @FXML
    public void initialize() {
        cargarMesas();
    }

    private void cargarMesas() {
        try {
            mesasActuales = mesaRepository.obtenerTodas();

            actualizarContadores();

            mostrarMesas("TODAS");

        } catch (SQLException e) {
            mostrarError("Error al cargar mesas: " + e.getMessage());
        }
    }

    private void mostrarMesas(String filtro) {

        // Elimina solamente los botones de las mesas
        // y conserva los elementos que pusimos en Scene Builder.
        panelMesas.getChildren().removeIf(
                nodo -> nodo.getId() != null &&
                        nodo.getId().startsWith("mesa-")
        );

        for (Mesa mesa : mesasActuales) {

            if (filtro.equals("TODAS") ||
                    mesa.getCodigoEstado().equals(filtro)) {

                Button boton = crearBotonMesa(mesa);

                boton.setId("mesa-" + mesa.getIdMesa());

                panelMesas.getChildren().add(boton);
            }
        }
    }

    private Button crearBotonMesa(Mesa mesa) {

        Button btn = new Button(
                "Mesa " + mesa.getNumeroMesa() +
                        "\n" + mesa.getNombreZona()
        );

        btn.setPrefSize(100, 80);

        // Posición fija de cada mesa
        switch (mesa.getNumeroMesa()) {

            case 1:
                btn.setLayoutX(50);
                btn.setLayoutY(50);
                break;

            case 2:
                btn.setLayoutX(200);
                btn.setLayoutY(50);
                break;

            case 3:
                btn.setLayoutX(350);
                btn.setLayoutY(50);
                break;

            case 4:
                btn.setLayoutX(50);
                btn.setLayoutY(200);
                break;

            case 5:
                btn.setLayoutX(200);
                btn.setLayoutY(200);
                break;

            case 6:
                btn.setLayoutX(350);
                btn.setLayoutY(200);
                break;
            case 7:
                btn.setLayoutX(50);
                btn.setLayoutY(350);
                break;

            case 8:
                btn.setLayoutX(200);
                btn.setLayoutY(350);
                break;

            case 9:
                btn.setLayoutX(350);
                btn.setLayoutY(350);
                break;

            default:
                btn.setLayoutX(50);
                btn.setLayoutY(350);
                break;
        }

        btn.setStyle(
                "-fx-background-color: " +
                        colorSegunEstado(mesa.getCodigoEstado()) +
                        "; -fx-text-fill: white; -fx-font-weight: bold;"
        );

        btn.setOnAction(e -> {
            mesaSeleccionada = mesa;
            mostrarDetalleMesa(mesa);
        });

        NotaMesaHelper.aplicarBadgeSiTieneNota(btn, mesa.getIdMesa());

        return btn;
    }

    private String colorSegunEstado(String codigoEstado) {

        return switch (codigoEstado) {
            case "LIBRE" -> "#4CAF50";
            case "OCUPADA" -> "#F44336";
            case "RESERVADA" -> "#FF9800";
            default -> "#2196F3";
        };
    }

    private void actualizarContadores() {

        int libres = 0;
        int ocupadas = 0;
        int reservadas = 0;

        for (Mesa mesa : mesasActuales) {

            switch (mesa.getCodigoEstado()) {

                case "LIBRE":
                    libres++;
                    break;

                case "OCUPADA":
                    ocupadas++;
                    break;

                case "RESERVADA":
                    reservadas++;
                    break;
            }
        }

        lblMesasLibres.setText("Libres: " + libres);
        lblMesasOcupadas.setText("Ocupadas: " + ocupadas);
        lblMesasReservadas.setText("Reservadas: " + reservadas);
    }

    @FXML
    private void mostrarTodas() {
        mostrarMesas("TODAS");
    }

    @FXML
    private void filtrarLibres() {
        mostrarMesas("LIBRE");
    }

    @FXML
    private void filtrarOcupadas() {
        mostrarMesas("OCUPADA");
    }

    @FXML
    private void filtrarReservadas() {
        mostrarMesas("RESERVADA");
    }

    @FXML
    private void buscarMesa() {
        BusquedaMesaHelper.buscarYResaltarMesa(panelMesas, txtBuscarMesa.getText());
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

                mostrarError(
                        "Error al agregar mesa: " +
                                e.getMessage()
                );
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

            mesaRepository.quitarMesa(
                    mesaSeleccionada.getIdMesa()
            );

            mesaSeleccionada = null;

            cargarMesas();

        } catch (SQLException e) {

            mostrarError(
                    "Error al quitar mesa: " +
                            e.getMessage()
            );
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
                        "Código: " + (mesa.getCodigoMesa() != null
                        ? mesa.getCodigoMesa()
                        : "N/A")
        );

        alert.getButtonTypes().clear();

        alert.getButtonTypes().addAll(
                new javafx.scene.control.ButtonType("Cambiar estado"),
                new javafx.scene.control.ButtonType("Cambiar capacidad"),
                new javafx.scene.control.ButtonType("Nota"),
                new javafx.scene.control.ButtonType("Cerrar")
        );

        Optional<javafx.scene.control.ButtonType> resultado =
                alert.showAndWait();

        if (resultado.isPresent()) {

            if (resultado.get().getText().equals("Cambiar estado")) {
                cambiarEstadoMesa(mesa);

            } else if (resultado.get().getText().equals("Cambiar capacidad")) {
                cambiarCapacidadMesa(mesa);
            } else if (resultado.get().getText().equals("Nota")) {
                NotaMesaHelper.mostrarDialogoNota(
                        mesa.getIdMesa(), mesa.getNumeroMesa(), this::cargarMesas
                );
            }
        }
    }
    private void cambiarEstadoMesa(Mesa mesa) {

        List<String> estados = List.of(
                "LIBRE",
                "OCUPADA",
                "RESERVADA"
        );

        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                mesa.getCodigoEstado(),
                estados
        );

        dialog.setTitle("Cambiar estado");
        dialog.setHeaderText("Mesa " + mesa.getNumeroMesa());
        dialog.setContentText("Seleccione el nuevo estado:");

        Optional<String> resultado = dialog.showAndWait();

        if (resultado.isPresent()) {
            try {
                mesaRepository.cambiarEstadoMesa(
                        mesa.getIdMesa(),
                        resultado.get()
                );

                cargarMesas();

            } catch (SQLException e) {
                mostrarError(
                        "Error al cambiar el estado: " + e.getMessage()
                );
            }
        }
    }

    private void mostrarError(String mensaje) {

        Alert alert = new Alert(Alert.AlertType.ERROR);

        alert.setContentText(mensaje);

        alert.showAndWait();
    }

    private void cambiarCapacidadMesa(Mesa mesa) {
        TextInputDialog dialog = new TextInputDialog(
                String.valueOf(mesa.getCapacidad())
        );

        dialog.setTitle("Cambiar capacidad");
        dialog.setHeaderText("Mesa " + mesa.getNumeroMesa());
        dialog.setContentText("Nueva capacidad:");

        Optional<String> resultado = dialog.showAndWait();

        if (resultado.isPresent()) {
            try {
                int capacidad = Integer.parseInt(resultado.get());

                if (capacidad <= 0) {
                    mostrarError("La capacidad debe ser mayor que 0.");
                    return;
                }

                mesaRepository.cambiarCapacidadMesa(
                        mesa.getIdMesa(),
                        capacidad
                );

                cargarMesas();

            } catch (NumberFormatException e) {
                mostrarError("Ingrese un número válido.");

            } catch (SQLException e) {
                mostrarError(
                        "Error al cambiar la capacidad: " + e.getMessage()
                );
            }
        }
    }
}