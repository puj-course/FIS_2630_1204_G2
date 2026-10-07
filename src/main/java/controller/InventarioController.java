package controller;

import entity.Ingrediente;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import repository.IngredienteRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class InventarioController {

    @FXML
    private TextField campoBusqueda;

    @FXML
    private TableView<Ingrediente> tablaIngredientes;

    @FXML
    private TableColumn<Ingrediente, Long> columnaId;

    @FXML
    private TableColumn<Ingrediente, String> columnaNombre;

    @FXML
    private TableColumn<Ingrediente, BigDecimal> columnaStock;

    @FXML
    private TableColumn<Ingrediente, BigDecimal> columnaStockMinimo;

    @FXML
    private TableColumn<Ingrediente, String> columnaEstado;

    @FXML
    private TableColumn<Ingrediente, String> columnaAlerta;

    @FXML
    private Label etiquetaTotal;

    @FXML
    private Label etiquetaStockBajo;

    @FXML
    private Label etiquetaEstado;

    @FXML
    private Button botonBuscar;

    @FXML
    private Button botonLimpiar;

    @FXML
    private Button botonActualizar;

    private final IngredienteRepository ingredienteRepository =
            new IngredienteRepository();

    private final ObservableList<Ingrediente> ingredientes =
            FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        configurarTabla();

        campoBusqueda.setOnAction(event -> buscar());

        tablaIngredientes.setItems(ingredientes);

        cargarIngredientes();
    }

    /**
     * Configura las columnas de la tabla de inventario.
     */
    private void configurarTabla() {

        columnaId.setCellValueFactory(
                new PropertyValueFactory<>("ingredienteId")
        );

        columnaNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre")
        );

        columnaStock.setCellValueFactory(
                new PropertyValueFactory<>("stockActual")
        );

        columnaStockMinimo.setCellValueFactory(
                new PropertyValueFactory<>("stockMinimo")
        );

        columnaEstado.setCellValueFactory(
                new PropertyValueFactory<>("estado")
        );

        columnaAlerta.setCellValueFactory(
                celda -> new javafx.beans.property.SimpleStringProperty(
                        obtenerTextoAlerta(celda.getValue())
                )
        );

        columnaStock.setCellFactory(
                columna -> new TableCell<>() {
                    @Override
                    protected void updateItem(
                            BigDecimal valor,
                            boolean empty
                    ) {
                        super.updateItem(valor, empty);

                        if (empty || valor == null) {
                            setText(null);
                        } else {
                            setText(valor.stripTrailingZeros().toPlainString());
                        }
                    }
                }
        );

        columnaStockMinimo.setCellFactory(
                columna -> new TableCell<>() {
                    @Override
                    protected void updateItem(
                            BigDecimal valor,
                            boolean empty
                    ) {
                        super.updateItem(valor, empty);

                        if (empty || valor == null) {
                            setText(null);
                        } else {
                            setText(valor.stripTrailingZeros().toPlainString());
                        }
                    }
                }
        );

        columnaAlerta.setCellFactory(
                columna -> new TableCell<>() {
                    @Override
                    protected void updateItem(
                            String valor,
                            boolean empty
                    ) {
                        super.updateItem(valor, empty);

                        if (empty || valor == null) {
                            setText(null);
                            setStyle("");
                            return;
                        }

                        setText(valor);

                        if ("STOCK BAJO".equals(valor)) {
                            setStyle(
                                    "-fx-font-weight: bold;"
                            );
                        } else {
                            setStyle("");
                        }
                    }
                }
        );

        tablaIngredientes.setRowFactory(
                tabla -> new TableRow<>() {

                    @Override
                    protected void updateItem(
                            Ingrediente ingrediente,
                            boolean empty
                    ) {
                        super.updateItem(
                                ingrediente,
                                empty
                        );

                        if (empty || ingrediente == null) {
                            setStyle("");
                            return;
                        }

                        if (ingrediente.tieneStockBajo()) {
                            setStyle(
                                    "-fx-background-color: #fff3cd;"
                            );
                        } else {
                            setStyle("");
                        }
                    }
                }
        );
    }

    /**
     * Carga todos los ingredientes activos desde la base de datos.
     */
    @FXML
    private void cargarIngredientes() {

        try {

            List<Ingrediente> lista =
                    ingredienteRepository.findAllActivos();

            ingredientes.setAll(lista);

            actualizarResumen();

            etiquetaEstado.setText(
                    "Inventario actualizado correctamente."
            );

        } catch (SQLException e) {

            mostrarError(
                    "No se pudo cargar el inventario.",
                    e
            );
        }
    }

    /**
     * Busca ingredientes por nombre.
     */
    @FXML
    private void buscar() {

        String texto =
                campoBusqueda.getText();

        if (texto == null || texto.isBlank()) {
            cargarIngredientes();
            return;
        }

        try {

            List<Ingrediente> resultados =
                    ingredienteRepository.buscarActivosPorNombre(
                            texto.trim()
                    );

            ingredientes.setAll(resultados);

            actualizarResumen();

            etiquetaEstado.setText(
                    resultados.size()
                            + " ingrediente(s) encontrado(s)."
            );

        } catch (SQLException e) {

            mostrarError(
                    "No se pudo realizar la búsqueda.",
                    e
            );
        }
    }

    /**
     * Limpia el campo de búsqueda y vuelve a mostrar
     * todos los ingredientes activos.
     */
    @FXML
    private void limpiarBusqueda() {

        campoBusqueda.clear();

        cargarIngredientes();
    }

    /**
     * Actualiza manualmente la información mostrada.
     */
    @FXML
    private void actualizar() {

        cargarIngredientes();
    }

    /**
     * Actualiza los indicadores superiores de la pantalla.
     */
    private void actualizarResumen() {

        int total =
                ingredientes.size();

        long stockBajo =
                ingredientes.stream()
                        .filter(Ingrediente::tieneStockBajo)
                        .count();

        etiquetaTotal.setText(
                "Ingredientes: " + total
        );

        etiquetaStockBajo.setText(
                "Stock bajo: " + stockBajo
        );
    }

    /**
     * Obtiene el texto mostrado en la columna de alerta.
     */
    private String obtenerTextoAlerta(
            Ingrediente ingrediente
    ) {

        if (ingrediente == null) {
            return "";
        }

        return ingrediente.tieneStockBajo()
                ? "STOCK BAJO"
                : "NORMAL";
    }

    /**
     * Muestra un mensaje de error al usuario.
     */
    private void mostrarError(
            String mensaje,
            Exception excepcion
    ) {

        etiquetaEstado.setText(
                "Ocurrió un error al consultar el inventario."
        );

        Alert alerta =
                new Alert(Alert.AlertType.ERROR);

        alerta.setTitle("Inventario");
        alerta.setHeaderText(mensaje);
        alerta.setContentText(
                excepcion.getMessage() != null
                        ? excepcion.getMessage()
                        : "No se obtuvo información adicional."
        );

        alerta.showAndWait();
    }
}