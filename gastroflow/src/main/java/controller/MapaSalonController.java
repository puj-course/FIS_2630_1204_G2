package controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
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
    private static final double INICIO_Y = 210;
    private static final double ESPACIO_X = 18;
    private static final double ESPACIO_Y = 18;
    private static final int COLUMNAS = 3;

    @FXML
    private Pane panelMesas;

    @FXML
    private VBox leyendaEstados;

    @FXML
    private ToggleButton filtroTodas;

    @FXML
    private ToggleButton filtroLibres;

    @FXML
    private ToggleButton filtroOcupadas;

    @FXML
    private ToggleButton filtroReservadas;

    @FXML
    private Label mensajeFiltro;

    @FXML
    private Label tituloDetalleMesa;

    @FXML
    private Label detalleNumero;

    @FXML
    private Label detalleEstado;

    @FXML
    private Label detalleCapacidad;

    @FXML
    private Label detalleComensales;

    @FXML
    private Label detallePedido;

    @FXML
    private Label detalleMesero;

    @FXML
    private Label detalleCodigo;

    private final MesaRepository mesaRepository = new MesaRepository();
    private final Map<Integer, Button> botonesPorMesa = new HashMap<>();

    private Mesa mesaSeleccionada;
    private Timeline actualizadorEstados;
    private EstadoMesaVisual filtroActivo;

    @FXML
    public void initialize() {
        configurarFiltros();
        construirLeyenda();
        limpiarDetalleMesa();
        cargarMesas();
        iniciarActualizacionAutomatica();
    }

    private void cargarMesas() {
        try {
            List<Mesa> mesas = mesaRepository.obtenerTodas();
            Set<Integer> mesasVisibles = new HashSet<>();
            int posicionVisible = 0;

            for (Mesa mesa : mesas) {
                if (!cumpleFiltro(mesa)) {
                    continue;
                }

                mesasVisibles.add(mesa.getIdMesa());
                Button boton = botonesPorMesa.get(mesa.getIdMesa());
                if (boton == null) {
                    boton = crearBotonMesa(mesa, posicionVisible);
                    botonesPorMesa.put(mesa.getIdMesa(), boton);
                    panelMesas.getChildren().add(boton);
                } else {
                    actualizarBotonMesa(boton, mesa, posicionVisible);
                }

                if (mesaSeleccionada != null && mesaSeleccionada.getIdMesa() == mesa.getIdMesa()) {
                    mesaSeleccionada = mesa;
                }

                posicionVisible++;
            }

            quitarMesasNoVisibles(mesasVisibles);
            actualizarMensajeFiltro(posicionVisible);
            if (mesaSeleccionada != null) {
                actualizarDetalleMesa(mesaSeleccionada);
            }
        } catch (SQLException e) {
            mostrarError("Error al cargar mesas: " + e.getMessage());
        }
    }

    private boolean cumpleFiltro(Mesa mesa) {
        return filtroActivo == null || filtroActivo == EstadoMesaVisual.desdeCodigo(mesa.getCodigoEstado());
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
        boolean seleccionada = mesaSeleccionada != null && mesaSeleccionada.getIdMesa() == mesa.getIdMesa();
        btn.setStyle(estado != null ? estiloMesa(estado, seleccionada) : estiloEstadoNoValido(seleccionada));
        btn.setOnAction(e -> {
            mesaSeleccionada = mesa;
            actualizarDetalleMesa(mesa);
            cargarMesas();
        });
    }

    private String estiloMesa(EstadoMesaVisual estado, boolean seleccionada) {
        return "-fx-background-color: " + estado.color + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-border-color: " + (seleccionada ? "#111111" : estado.color) + ";" +
                "-fx-border-width: " + (seleccionada ? "3" : "1") + ";" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;";
    }

    private String estiloEstadoNoValido(boolean seleccionada) {
        return "-fx-background-color: white;" +
                "-fx-text-fill: #B00020;" +
                "-fx-font-weight: bold;" +
                "-fx-border-color: " + (seleccionada ? "#111111" : "#B00020") + ";" +
                "-fx-border-width: " + (seleccionada ? "3" : "2") + ";" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;";
    }

    private void quitarMesasNoVisibles(Set<Integer> mesasVisibles) {
        botonesPorMesa.entrySet().removeIf(entry -> {
            boolean noVisible = !mesasVisibles.contains(entry.getKey());
            if (noVisible) {
                panelMesas.getChildren().remove(entry.getValue());
            }
            return noVisible;
        });

        if (mesaSeleccionada != null && !mesasVisibles.contains(mesaSeleccionada.getIdMesa())) {
            mesaSeleccionada = null;
            limpiarDetalleMesa();
        }
    }

    private void actualizarMensajeFiltro(int cantidadVisible) {
        if (cantidadVisible == 0) {
            mensajeFiltro.setText("No se encontraron mesas para el filtro seleccionado.");
            mensajeFiltro.setVisible(true);
        } else {
            mensajeFiltro.setText("");
            mensajeFiltro.setVisible(false);
        }
    }

    private void configurarFiltros() {
        ToggleGroup grupoFiltros = new ToggleGroup();
        filtroTodas.setToggleGroup(grupoFiltros);
        filtroLibres.setToggleGroup(grupoFiltros);
        filtroOcupadas.setToggleGroup(grupoFiltros);
        filtroReservadas.setToggleGroup(grupoFiltros);
        filtroTodas.setSelected(true);

        filtroTodas.setOnAction(event -> aplicarFiltro(null, filtroTodas));
        filtroLibres.setOnAction(event -> aplicarFiltro(EstadoMesaVisual.DISPONIBLE, filtroLibres));
        filtroOcupadas.setOnAction(event -> aplicarFiltro(EstadoMesaVisual.OCUPADA, filtroOcupadas));
        filtroReservadas.setOnAction(event -> aplicarFiltro(EstadoMesaVisual.RESERVADA, filtroReservadas));
    }

    private void aplicarFiltro(EstadoMesaVisual nuevoFiltro, ToggleButton botonSeleccionado) {
        filtroActivo = nuevoFiltro;
        botonSeleccionado.setSelected(true);
        cargarMesas();
    }

    private void actualizarDetalleMesa(Mesa mesa) {
        EstadoMesaVisual estado = EstadoMesaVisual.desdeCodigo(mesa.getCodigoEstado());
        boolean ocupada = estado == EstadoMesaVisual.OCUPADA;

        tituloDetalleMesa.setText("Detalle de Mesa " + mesa.getNumeroMesa());
        detalleNumero.setText("Numero: " + mesa.getNumeroMesa());
        detalleEstado.setText("Estado: " + (estado != null ? estado.etiqueta : "Estado no valido"));
        detalleCapacidad.setText("Capacidad maxima: " + mesa.getCapacidad() + " personas");
        detalleComensales.setText("Comensales: " + textoComensales(mesa, ocupada));
        detallePedido.setText("Pedido activo: " + textoOpcional(mesa.getPedidoActivo(), "Sin pedido activo"));
        detalleMesero.setText("Mesero responsable: " + textoOpcional(mesa.getMeseroResponsable(), "Sin mesero asignado"));
        detalleCodigo.setText("Codigo: " + textoOpcional(mesa.getCodigoMesa(), "N/A"));
    }

    private void limpiarDetalleMesa() {
        tituloDetalleMesa.setText("Seleccione una mesa");
        detalleNumero.setText("Numero: -");
        detalleEstado.setText("Estado: -");
        detalleCapacidad.setText("Capacidad maxima: -");
        detalleComensales.setText("Comensales: -");
        detallePedido.setText("Pedido activo: -");
        detalleMesero.setText("Mesero responsable: -");
        detalleCodigo.setText("Codigo: -");
    }

    private String textoComensales(Mesa mesa, boolean ocupada) {
        if (!ocupada) {
            return "No aplica";
        }
        return mesa.getCantidadComensales() != null ? mesa.getCantidadComensales().toString() : "No registrado";
    }

    private String textoOpcional(String valor, String textoVacio) {
        return valor != null && !valor.isBlank() ? valor : textoVacio;
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
