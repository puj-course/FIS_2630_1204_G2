package controller;

import dao.DisponibilidadDAO;
import dao.ProductoDAO;
import dto.ItemPedido;
import entity.Producto;
import util.TextoBusqueda;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
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

    // HU-39
    @FXML private TextField busquedaField;
    @FXML private ComboBox<String> categoriaCombo;
    @FXML private CheckBox soloDisponiblesCheck;
    @FXML private Label resultadosLabel;

    private static final String TODAS_LAS_CATEGORIAS = "Todas";

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final DisponibilidadDAO disponibilidadDAO = new DisponibilidadDAO();
    private final Map<Long, ItemPedido> pedido = new LinkedHashMap<>();
    private final NumberFormat moneda = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));

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
     * Relee el menú desde la base y vuelve a pintarlo respetando los filtros.
     *
     * Va en un hilo aparte: recalcular la disponibilidad recorre todo el
     * catálogo, y si se hiciera en el hilo de JavaFX la ventana quedaría
     * congelada mientras tanto — y para siempre si la base no responde.
     */
    @FXML
    private void cargarMenu() {
        Task<Map<String, List<Producto>>> tarea = new Task<>() {
            @Override
            protected Map<String, List<Producto>> call() throws SQLException {
                return productoDAO.obtenerMenuPorCategorias();
            }
        };

        tarea.setOnSucceeded(e -> {
            menuCompleto = tarea.getValue();
            actualizarCategorias();
            mostrarMenuFiltrado();
        });

        tarea.setOnFailed(e -> {
            categoriasBox.getChildren().clear();
            resultadosLabel.setText("");
            mostrarError(tarea.getException());
        });

        mostrarMensajeEnMenu("Cargando menú…");
        iniciar(tarea, "carga-menu");
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

        String busqueda = busquedaField.getText();
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
                if (TextoBusqueda.coincide(p.getNombre(), p.getDescripcion(), busqueda)) {
                    coincidencias.add(p);
                }
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
            mostrarMensajeEnMenu("Ningún producto coincide con la búsqueda.");
        }

        resultadosLabel.setText(visibles == 1 ? "1 producto" : visibles + " productos");
    }

    private void mostrarMensajeEnMenu(String mensaje) {
        Label etiqueta = new Label(mensaje);
        etiqueta.getStyleClass().add("subtitulo");
        categoriasBox.getChildren().setAll(etiqueta);
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
                // Solo se rechaza la unidad que no alcanza. Las que ya estaban en
                // el pedido se conservan: antes se borraba la línea entera y el
                // mesero perdía las tres hamburguesas por pedir una cuarta.
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("No hay inventario suficiente");

                if (existente == null) {
                    alert.setContentText(producto.getNombre()
                            + " ya no tiene ingredientes suficientes y no puede agregarse.");
                    cargarMenu();
                } else {
                    alert.setContentText("No alcanza para " + cantidadSolicitada + " unidades de "
                            + producto.getNombre() + ". Se mantienen las "
                            + existente.getCantidad() + " que ya tenía en el pedido.");
                }

                alert.showAndWait();
                return;
            }

            if (existente == null) pedido.put(producto.getProductoId(), new ItemPedido(producto));
            else existente.aumentarCantidad();
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

    /**
     * Valida el pedido completo contra el inventario.
     *
     * Se pregunta una sola vez por todo el pedido, no producto por producto:
     * dos platos distintos pueden compartir un ingrediente y pasar cada uno por
     * separado aunque juntos no alcancen.
     */
    @FXML
    private void confirmarPedido() {
        if (pedido.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Debe agregar al menos un producto.").showAndWait();
            return;
        }

        Map<Long, Integer> lineas = new LinkedHashMap<>();
        for (ItemPedido item : pedido.values()) {
            lineas.put(item.getProducto().getProductoId(), item.getCantidad());
        }

        Task<List<String>> tarea = new Task<>() {
            @Override
            protected List<String> call() throws SQLException {
                return disponibilidadDAO.faltantesDelPedido(lineas);
            }
        };

        tarea.setOnSucceeded(e -> {
            List<String> faltantes = tarea.getValue();

            if (faltantes.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION,
                        "Todos los productos siguen disponibles. El pedido puede continuar "
                                + "con la HU de registro de pedidos.").showAndWait();
                return;
            }

            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setHeaderText("El pedido completo no se puede preparar");
            alerta.setContentText("Falta inventario para:\n\n• " + String.join("\n• ", faltantes));
            alerta.showAndWait();

            cargarMenu();
        });

        tarea.setOnFailed(e -> mostrarError(tarea.getException()));

        iniciar(tarea, "validar-pedido");
    }

    private void iniciar(Task<?> tarea, String nombreHilo) {
        Thread hilo = new Thread(tarea, nombreHilo);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarError(Throwable e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Error de base de datos");
        alert.setContentText(e == null ? "Error desconocido" : e.getMessage());
        alert.showAndWait();
    }
}
