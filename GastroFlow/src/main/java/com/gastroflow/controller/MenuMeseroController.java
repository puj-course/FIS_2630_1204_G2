package com.gastroflow.controller;

import com.gastroflow.dao.ProductoDAO;
import com.gastroflow.model.ItemPedido;
import com.gastroflow.model.Producto;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.*;

public class MenuMeseroController {
    @FXML private VBox categoriasBox;
    @FXML private VBox pedidoBox;
    @FXML private Label totalLabel;

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final Map<Long, ItemPedido> pedido = new LinkedHashMap<>();
    private final NumberFormat moneda = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @FXML
    public void initialize() {
        cargarMenu();
        actualizarPedido();
    }

    @FXML
    private void cargarMenu() {
        categoriasBox.getChildren().clear();
        try {
            Map<String, List<Producto>> menu = productoDAO.obtenerMenuPorCategorias();
            for (Map.Entry<String, List<Producto>> entrada : menu.entrySet()) {
                Label titulo = new Label(entrada.getKey());
                titulo.getStyleClass().add("categoria-titulo");

                TilePane pane = new TilePane();
                pane.setHgap(14);
                pane.setVgap(14);
                pane.setPrefColumns(3);

                for (Producto p : entrada.getValue()) pane.getChildren().add(crearTarjeta(p));
                categoriasBox.getChildren().addAll(titulo, pane);
            }
        } catch (SQLException e) {
            mostrarError(e);
        }
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

        card.getChildren().addAll(nombre, precio, estado, agregar);
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
