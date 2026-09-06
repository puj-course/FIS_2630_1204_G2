package com.gastroflow.controller;

import com.gastroflow.dao.ProductoDAO;
import com.gastroflow.model.ItemPedido;
import com.gastroflow.model.Producto;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.util.*;

public class MenuMeseroController {
    @FXML private VBox categoriasBox;
    @FXML private VBox pedidoBox;
    @FXML private Label totalLabel;

    // HU-39
    @FXML private TextField busquedaField;
    @FXML private ComboBox<String> categoriaCombo;
    @FXML private CheckBox soloDisponiblesCheck;
    @FXML private Label resultadosLabel;

    private static final String TODAS_LAS_CATEGORIAS = "Todas";

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final Map<Long, ItemPedido> pedido = new LinkedHashMap<>();
    private final NumberFormat moneda = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    /** Menú completo tal como vino de la base. Los filtros trabajan sobre esta copia. */
    private Map<String, List<Producto>> menuCompleto = new LinkedHashMap<>();

    @FXML
    public void initialize() {
        busquedaField.textProperty().addListener((obs, anterior, actual) -> mostrarMenuFiltrado());
        categoriaCombo.valueProperty().addListener((obs, anterior, actual) -> mostrarMenuFiltrado());
        soloDisponiblesCheck.selectedProperty().addListener((obs, anterior, actual) -> mostrarMenuFiltrado());

        cargarMenu();
        actualizarPedido();
    }

    /**
     * Relee el menú desde la base de datos y vuelve a pintarlo respetando los
     * filtros que el mesero tenga puestos.
     */
    @FXML
    private void cargarMenu() {
        try {
            menuCompleto = productoDAO.obtenerMenuPorCategorias();
            actualizarCategorias();
            mostrarMenuFiltrado();
        } catch (SQLException e) {
            mostrarError(e);
        }
    }

    @FXML
    private void limpiarFiltros() {
        busquedaField.clear();
        soloDisponiblesCheck.setSelected(false);
        categoriaCombo.setValue(TODAS_LAS_CATEGORIAS);
    }

    /** Rellena el combo con las categorías del menú, conservando la selección actual. */
    private void actualizarCategorias() {
        String seleccionada = categoriaCombo.getValue();

        List<String> categorias = new ArrayList<>();
        categorias.add(TODAS_LAS_CATEGORIAS);
        categorias.addAll(menuCompleto.keySet());
        categoriaCombo.setItems(FXCollections.observableArrayList(categorias));

        categoriaCombo.setValue(
                categorias.contains(seleccionada) ? seleccionada : TODAS_LAS_CATEGORIAS);
    }

    /** Pinta el menú aplicando búsqueda, categoría y el filtro de disponibilidad. */
    private void mostrarMenuFiltrado() {
        categoriasBox.getChildren().clear();

        String busqueda = normalizar(busquedaField.getText());
        String categoria = categoriaCombo.getValue();
        boolean soloDisponibles = soloDisponiblesCheck.isSelected();

        int visibles = 0;

        for (Map.Entry<String, List<Producto>> entrada : menuCompleto.entrySet()) {
            if (categoria != null
                    && !TODAS_LAS_CATEGORIAS.equals(categoria)
                    && !categoria.equals(entrada.getKey())) {
                continue;
            }

            List<Producto> coincidencias = new ArrayList<>();
            for (Producto p : entrada.getValue()) {
                if (soloDisponibles && !p.isDisponible()) continue;
                if (coincide(p, busqueda)) coincidencias.add(p);
            }

            if (coincidencias.isEmpty()) continue;

            Label titulo = new Label(entrada.getKey() + "  (" + coincidencias.size() + ")");
            titulo.getStyleClass().add("categoria-titulo");

            TilePane pane = new TilePane();
            pane.setHgap(14);
            pane.setVgap(14);
            pane.setPrefColumns(3);

            for (Producto p : coincidencias) pane.getChildren().add(crearTarjeta(p));

            categoriasBox.getChildren().addAll(titulo, pane);
            visibles += coincidencias.size();
        }

        if (visibles == 0) {
            Label vacio = new Label("Ningún producto coincide con la búsqueda.");
            vacio.getStyleClass().add("subtitulo");
            categoriasBox.getChildren().add(vacio);
        }

        resultadosLabel.setText(visibles == 1 ? "1 producto" : visibles + " productos");
    }

    /** Busca el texto en el nombre y en la descripción, sin distinguir tildes ni mayúsculas. */
    private boolean coincide(Producto producto, String busquedaNormalizada) {
        if (busquedaNormalizada.isEmpty()) return true;

        return normalizar(producto.getNombre()).contains(busquedaNormalizada)
                || normalizar(producto.getDescripcion()).contains(busquedaNormalizada);
    }

    /** Pasa a minúsculas y quita tildes, para que "Ají" encuentre "aji". */
    private String normalizar(String texto) {
        if (texto == null) return "";

        return Normalizer.normalize(texto.trim().toLowerCase(new Locale("es", "CO")),
                        Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private VBox crearTarjeta(Producto producto) {
        VBox card = new VBox(8);
        card.setPrefWidth(205);
        card.setMinHeight(155);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("producto-card");

        Label nombre = new Label(producto.getNombre());
        nombre.setWrapText(true);
        nombre.getStyleClass().add("producto-nombre");
        Label precio = new Label(moneda.format(producto.getPrecioVenta()));
        Label estado = new Label(producto.isDisponible() ? "Disponible" : "AGOTADO");
        estado.getStyleClass().add(producto.isDisponible() ? "disponible" : "agotado");
        Button agregar = new Button(producto.isDisponible() ? "Agregar" : "No disponible");
        agregar.setMaxWidth(Double.MAX_VALUE);
        agregar.setDisable(!producto.isDisponible());
        agregar.setOnAction(e -> agregarProducto(producto));

        card.getChildren().addAll(nombre, precio);

        // HU-39: la descripción ya venía del DAO pero no se mostraba.
        if (producto.getDescripcion() != null && !producto.getDescripcion().isBlank()) {
            Label descripcion = new Label(producto.getDescripcion());
            descripcion.setWrapText(true);
            descripcion.getStyleClass().add("producto-descripcion");
            card.getChildren().add(descripcion);
        }

        card.getChildren().addAll(estado, agregar);
        return card;
    }

    private void agregarProducto(Producto producto) {
        try {
            ItemPedido existente = pedido.get(producto.getProductoId());
            int cantidadSolicitada = existente == null ? 1 : existente.getCantidad() + 1;

            if (!productoDAO.estaDisponible(producto.getProductoId(), cantidadSolicitada)) {
                pedido.remove(producto.getProductoId());
                actualizarPedido();
                cargarMenu();
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("Producto agotado");
                alert.setContentText(producto.getNombre() + " ya no tiene ingredientes suficientes y no puede agregarse.");
                alert.showAndWait();
                return;
            }

            ItemPedido item = pedido.get(producto.getProductoId());
            if (item == null) pedido.put(producto.getProductoId(), new ItemPedido(producto));
            else item.aumentarCantidad();
            actualizarPedido();
        } catch (SQLException e) {
            mostrarError(e);
        }
    }

    private void actualizarPedido() {
        pedidoBox.getChildren().clear();
        BigDecimal total = BigDecimal.ZERO;
        if (pedido.isEmpty()) {
            pedidoBox.getChildren().add(new Label("No hay productos agregados."));
        } else {
            for (ItemPedido item : pedido.values()) {
                HBox fila = new HBox(8);
                fila.setAlignment(Pos.CENTER_LEFT);
                Label texto = new Label(item.getProducto().getNombre() + " × " + item.getCantidad());
                Button menos = new Button("−");
                Button mas = new Button("+");
                Button eliminar = new Button("Eliminar");
                mas.setOnAction(e -> agregarProducto(item.getProducto()));
                menos.setOnAction(e -> {
                    if (item.getCantidad() == 1) pedido.remove(item.getProducto().getProductoId());
                    else item.disminuirCantidad();
                    actualizarPedido();
                });
                eliminar.setOnAction(e -> {
                    pedido.remove(item.getProducto().getProductoId());
                    actualizarPedido();
                });
                fila.getChildren().addAll(texto, menos, mas, eliminar);
                pedidoBox.getChildren().add(fila);
                total = total.add(item.getSubtotal());
            }
        }
        totalLabel.setText(moneda.format(total));
    }

    @FXML
    private void confirmarPedido() {
        if (pedido.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Debe agregar al menos un producto.").showAndWait();
            return;
        }

        List<Long> agotados = new ArrayList<>();
        try {
            for (ItemPedido item : pedido.values()) {
                if (!productoDAO.estaDisponible(
                        item.getProducto().getProductoId(), item.getCantidad())) {
                    agotados.add(item.getProducto().getProductoId());
                }
            }
        } catch (SQLException e) {
            mostrarError(e);
            return;
        }

        if (!agotados.isEmpty()) {
            agotados.forEach(pedido::remove);
            actualizarPedido();
            cargarMenu();
            new Alert(Alert.AlertType.WARNING,
                    "Uno o más productos se agotaron antes de confirmar. Fueron retirados del pedido.").showAndWait();
            return;
        }

        new Alert(Alert.AlertType.INFORMATION,
                "Todos los productos siguen disponibles. El pedido puede continuar con la HU de registro de pedidos.").showAndWait();
    }

    private void mostrarError(SQLException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Error de base de datos");
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}
