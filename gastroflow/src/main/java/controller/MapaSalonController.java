package controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import entity.Mesa;
import repository.MesaRepository;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class MapaSalonController {

    private static final double MESA_ANCHO = 120;
    private static final double MESA_ALTO = 78;
    private static final double INICIO_X = 24;
    private static final double INICIO_Y = 150;
    private static final double ESPACIO_X = 18;
    private static final double ESPACIO_Y = 18;
    private static final int COLUMNAS = 3;

    @FXML
    private Pane panelMesas;

    @FXML
    private VBox leyendaEstados;

    private final MesaRepository mesaRepository = new MesaRepository();
    private final Map<Integer, Button> botonesPorMesa = new HashMap<>();

    private Mesa mesaSeleccionada;
    private Timeline actualizadorEstados;

    @FXML
    public void initialize() {
        construirLeyenda();
        cargarMesas();
        iniciarActualizacionAutomatica();
    }

    private void cargarMesas() {
        try {
            List<Mesa> mesas = mesaRepository.obtenerTodas();
            Set<Integer> mesasActivas = new HashSet<>();

            for (int i = 0; i < mesas.size(); i++) {
                Mesa mesa = mesas.get(i);
                mesasActivas.add(mesa.getIdMesa());

                Button boton = botonesPorMesa.get(mesa.getIdMesa());
                if (boton == null) {
                    boton = crearBotonMesa(mesa, i);
                    botonesPorMesa.put(mesa.getIdMesa(), boton);
                    panelMesas.getChildren().add(boton);
                } else {
                    actualizarBotonMesa(boton, mesa, i);
                }

                if (mesaSeleccionada != null && mesaSeleccionada.getIdMesa() == mesa.getIdMesa()) {
                    mesaSeleccionada = mesa;
                }
            }

            quitarMesasInactivas(mesasActivas);
        } catch (SQLException e) {
            mostrarError("Error al cargar mesas: " + e.getMessage());
        }
    }

    private Button crearBotonMesa(Mesa mesa, int posicion) {
        Button btn = new Button();
        btn.setId("mesa-" + mesa.getIdMesa());
        btn.setPrefSize(MESA_ANCHO, MESA_ALTO);
        btn.setMinSize(MESA_ANCHO, MESA_ALTO);
        btn.setMaxSize(MESA_ANCHO, MESA_ALTO);
        btn.setWrapText(true);

        actualizarBotonMesa(btn, mesa, posicion);

        return btn;
    }

    private void actualizarBotonMesa(Button btn, Mesa mesa, int posicion) {
        EstadoMesaVisual estado = EstadoMesaVisual.desdeCodigo(mesa.getCodigoEstado());
        String textoEstado = estado != null ? estado.etiqueta : "Estado no valido";

        btn.setText(
                "Mesa " + mesa.getNumeroMesa() + "\n" +
                        textoEstado + "\n" +
                        Objects.toString(mesa.getNombreZona(), "Sin zona")
        );
        btn.setLayoutX(INICIO_X + (posicion % COLUMNAS) * (MESA_ANCHO + ESPACIO_X));
        btn.setLayoutY(INICIO_Y + (posicion / COLUMNAS) * (MESA_ALTO + ESPACIO_Y));
        btn.setStyle(estado != null ? estiloMesa(estado) : estiloEstadoNoValido());
        btn.setOnAction(e -> {
            mesaSeleccionada = mesa;
            mostrarDetalleMesa(mesa);
        });
    }

    private String estiloMesa(EstadoMesaVisual estado) {
        return "-fx-background-color: " + estado.color + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;";
    }

    private String estiloEstadoNoValido() {
        return "-fx-background-color: white;" +
                "-fx-text-fill: #B00020;" +
                "-fx-font-weight: bold;" +
                "-fx-border-color: #B00020;" +
                "-fx-border-width: 2;" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;";
    }

    private void quitarMesasInactivas(Set<Integer> mesasActivas) {
        botonesPorMesa.entrySet().removeIf(entry -> {
            boolean inactiva = !mesasActivas.contains(entry.getKey());
            if (inactiva) {
                panelMesas.getChildren().remove(entry.getValue());
            }
            return inactiva;
        });

        if (mesaSeleccionada != null && !mesasActivas.contains(mesaSeleccionada.getIdMesa())) {
            mesaSeleccionada = null;
        }
    }

    private void construirLeyenda() {
        leyendaEstados.getChildren().clear();

        for (EstadoMesaVisual estado : EstadoMesaVisual.values()) {
            Region color = new Region();
            color.setPrefSize(16, 16);
            color.setStyle("-fx-background-color: " + estado.color + "; -fx-background-radius: 3;");

            Label texto = new Label(estado.etiqueta);
            HBox fila = new HBox(8, color, texto);
            fila.setStyle("-fx-alignment: center-left;");
            leyendaEstados.getChildren().add(fila);
        }
    }

    private void iniciarActualizacionAutomatica() {
        actualizadorEstados = new Timeline(
                new KeyFrame(Duration.seconds(5), event -> cargarMesas())
        );
        actualizadorEstados.setCycleCount(Timeline.INDEFINITE);
        actualizadorEstados.play();
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
        EstadoMesaVisual estado = EstadoMesaVisual.desdeCodigo(mesa.getCodigoEstado());

        alert.setTitle("Detalle de Mesa");
        alert.setHeaderText("Mesa " + mesa.getNumeroMesa());

        alert.setContentText(
                "Zona: " + mesa.getNombreZona() + "\n" +
                        "Capacidad: " + mesa.getCapacidad() + " personas\n" +
                        "Estado: " + (estado != null ? estado.etiqueta : "Estado no valido") + "\n" +
                        "Codigo de estado: " + mesa.getCodigoEstado() + "\n" +
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

    private enum EstadoMesaVisual {
        DISPONIBLE("Disponible", "#2E7D32", "LIBRE", "DISPONIBLE"),
        OCUPADA("Ocupada", "#C62828", "OCUPADA"),
        RESERVADA("Reservada", "#EF6C00", "RESERVADA"),
        PENDIENTE_PAGO("Pendiente de pago", "#1565C0", "PENDIENTE_PAGO", "PENDIENTE_DE_PAGO"),
        INHABILITADA("Inhabilitada", "#616161", "INHABILITADA", "INHABILITADO", "MANTENIMIENTO");

        private final String etiqueta;
        private final String color;
        private final Set<String> codigosValidos;

        EstadoMesaVisual(String etiqueta, String color, String... codigosValidos) {
            this.etiqueta = etiqueta;
            this.color = color;
            this.codigosValidos = Set.of(codigosValidos);
        }

        private static EstadoMesaVisual desdeCodigo(String codigo) {
            if (codigo == null || codigo.isBlank()) {
                return null;
            }

            String normalizado = codigo.trim().toUpperCase().replace(' ', '_').replace('-', '_');
            for (EstadoMesaVisual estado : values()) {
                if (estado.codigosValidos.contains(normalizado)) {
                    return estado;
                }
            }
            return null;
        }
    }
}
