package com.gastroflow.controller;

import com.gastroflow.dao.DisponibilidadDAO;
import com.gastroflow.model.ProductoDisponibilidad;
import javafx.collections.FXCollections;
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

    @FXML
    private void recalcularYCargar() {
        try {
            dao.recalcularTodos();
            List<ProductoDisponibilidad> productos = dao.listarProductos();
            tablaProductos.setItems(FXCollections.observableArrayList(productos));

            long disponibles = productos.stream().filter(p -> "DISPONIBLE".equals(p.getEstado())).count();
            long agotados = productos.stream().filter(p -> "AGOTADO".equals(p.getEstado())).count();
            lblDisponibles.setText("Disponibles: " + disponibles);
            lblAgotados.setText("Agotados: " + agotados);
        } catch (SQLException e) {
            mostrarError(e);
        }
    }

    private void mostrarError(SQLException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error de base de datos");
        alert.setHeaderText("No fue posible validar la disponibilidad");
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}
