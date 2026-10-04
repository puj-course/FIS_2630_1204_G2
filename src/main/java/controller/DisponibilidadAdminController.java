package controller;

import dao.DisponibilidadDAO;
import dto.ProductoDisponibilidad;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.List;

public class DisponibilidadAdminController {
    @FXML private TableView<ProductoDisponibilidad> tablaProductos;
    @FXML private TableColumn<ProductoDisponibilidad, String> colCodigo;
    @FXML private TableColumn<ProductoDisponibilidad, String> colNombre;
    @FXML private TableColumn<ProductoDisponibilidad, String> colCategoria;
    @FXML private TableColumn<ProductoDisponibilidad, String> colEstado;
    @FXML private TableColumn<ProductoDisponibilidad, String> colMotivo;
    @FXML private Label lblDisponibles;
    @FXML private Label lblAgotados;

    private final DisponibilidadDAO dao = new DisponibilidadDAO();

    @FXML
    public void initialize() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colMotivo.setCellValueFactory(new PropertyValueFactory<>("motivo"));

        colEstado.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                setText(empty ? null : estado);
                getStyleClass().removeAll("estado-disponible", "estado-agotado");
                if (!empty && estado != null) {
                    getStyleClass().add("DISPONIBLE".equals(estado) ? "estado-disponible" : "estado-agotado");
                }
            }
        });

        recalcularYCargar();
    }

    /**
     * Recalcula el catálogo y refresca la tabla.
     *
     * Va en un hilo aparte: el recálculo recorre todos los productos, y si se
     * hiciera en el hilo de JavaFX la ventana no terminaría de abrirse hasta
     * que la base respondiera — o nunca, si está caída.
     */
    @FXML
    private void recalcularYCargar() {
        Task<List<ProductoDisponibilidad>> tarea = new Task<>() {
            @Override
            protected List<ProductoDisponibilidad> call() throws SQLException {
                dao.recalcularTodos();
                return dao.listarProductos();
            }
        };

        tarea.setOnSucceeded(e -> {
            List<ProductoDisponibilidad> productos = tarea.getValue();
            tablaProductos.setItems(FXCollections.observableArrayList(productos));

            long disponibles = productos.stream().filter(p -> "DISPONIBLE".equals(p.getEstado())).count();
            long agotados = productos.stream().filter(p -> "AGOTADO".equals(p.getEstado())).count();
            lblDisponibles.setText("Disponibles: " + disponibles);
            lblAgotados.setText("Agotados: " + agotados);
            tablaProductos.setPlaceholder(new Label("No hay productos registrados."));
        });

        tarea.setOnFailed(e -> {
            tablaProductos.setItems(FXCollections.observableArrayList());
            tablaProductos.setPlaceholder(new Label("No se pudo consultar la base de datos."));
            lblDisponibles.setText("Disponibles: —");
            lblAgotados.setText("Agotados: —");
            mostrarError(tarea.getException());
        });

        tablaProductos.setPlaceholder(new Label("Recalculando disponibilidad…"));

        Thread hilo = new Thread(tarea, "recalcular-disponibilidad");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarError(Throwable e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de base de datos");
        alert.setHeaderText("No fue posible validar la disponibilidad");
        alert.setContentText(e == null ? "Error desconocido" : e.getMessage());
        alert.showAndWait();
    }
}
