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
import entity.Pedido;
import service.PedidoService;
import service.InventarioService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MenuMeseroController {

    @FXML private VBox categoriasBox;
    @FXML private VBox pedidoBox;
    @FXML private Label totalLabel;

    // HU-39
    @FXML private TextField busquedaField;
    @FXML private ComboBox<String> categoriaCombo;
    @FXML private CheckBox soloDisponiblesCheck;
    @FXML private Label resultadosLabel;

    private final PedidoService pedidoService = new PedidoService();
    private final InventarioService inventarioService =
            new InventarioService();
    private Long mesaId;
    private Long usuarioId;
    private Pedido pedidoActual;

    private static final String TODAS_LAS_CATEGORIAS = "Todas";

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final DisponibilidadDAO disponibilidadDAO =
            new DisponibilidadDAO();

    private final Map<Long, ItemPedido> pedido =
            new LinkedHashMap<>();

    private final NumberFormat moneda =
            NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));

    /** Menú completo cargado desde la base de datos. */
    private Map<String, List<Producto>> menuCompleto =
            new LinkedHashMap<>();

    /**
     * Evita cargar varias veces el mismo pedido activo.
     */
    private boolean pedidoActivoCargado = false;

    public void configurarMesa(long mesaId, long usuarioId) {

        this.mesaId = mesaId;
        this.usuarioId = usuarioId;

        System.out.println("======================================");
        System.out.println(" PEDIDO CONFIGURADO");
        System.out.println(" Mesa: " + mesaId);
        System.out.println(" Usuario: " + usuarioId);
        System.out.println("======================================");

        /*
         * initialize() se ejecuta antes de configurarMesa().
         *
         * Por eso puede ocurrir que el menú todavía no haya terminado
         * de cargarse cuando llegamos aquí.
         *
         * Si el menú ya está disponible, cargamos inmediatamente
         * el pedido activo.
         */
        if (!menuCompleto.isEmpty()) {
            cargarPedidoActivo();
        }
    }

    @FXML
    public void initialize() {

        busquedaField.textProperty().addListener((obs, anterior, actual) -> mostrarMenuFiltrado());
        categoriaCombo.valueProperty().addListener((obs, anterior, actual) -> mostrarMenuFiltrado());
        soloDisponiblesCheck.selectedProperty().addListener((obs, anterior, actual) -> mostrarMenuFiltrado());
        cargarMenu();
        actualizarPedido();
    }

    /**
     * Carga el menú desde la base de datos.
     */
    @FXML
    private void cargarMenu() {

        Task<Map<String, List<Producto>>> tarea = new Task<>() {
            @Override
            protected Map<String, List<Producto>> call()
                    throws SQLException {
                        System.out.println(">>> INICIANDO CARGA DEL MENU");
                        Map<String, List<Producto>> resultado = productoDAO.obtenerMenuPorCategorias();
                        System.out.println(">>> MENU CARGADO CORRECTAMENTE");
                        System.out.println(">>> CATEGORIAS: " + resultado.keySet());
                        return resultado;
                    }
                };

        tarea.setOnSucceeded(e -> {
            System.out.println(">>> ENTRANDO A setOnSucceeded");
            menuCompleto = tarea.getValue();
            System.out.println(">>> menuCompleto asignado");
            actualizarCategorias();
            System.out.println(">>> actualizarCategorias TERMINO");
            mostrarMenuFiltrado();
            System.out.println(">>> mostrarMenuFiltrado TERMINO");
            /*
             * IMPORTANTE:
             *
             * configurarMesa() puede haberse ejecutado antes
             * de que termine esta carga.
             *
             * Cuando el menú ya está disponible podemos buscar
             * el pedido activo.
             */
            if (mesaId != null && !pedidoActivoCargado) {cargarPedidoActivo();
            }
        });

        tarea.setOnFailed(e -> {System.out.println(">>> TASK FALLO");
            if (e.getSource().getException() != null) {
                e.getSource().getException().printStackTrace();
            }
            mostrarError(tarea.getException());
        });

        mostrarMensajeEnMenu("Cargando menú…");
        iniciar(tarea, "carga-menu");
    }

    /**
     * Busca el pedido activo de la mesa y carga sus productos
     * dentro del mapa pedido.
     */
    private void cargarPedidoActivo() {

        if (pedidoActivoCargado) {
            return;
        }

        if (mesaId == null) {
            return;
        }

        /*
         * El menú debe estar disponible porque necesitamos
         * encontrar los objetos Producto correspondientes
         * a los IDs almacenados en el JSON.
         */
        if (menuCompleto.isEmpty()) {
            return;
        }

        pedidoActivoCargado = true;

        Task<Optional<Pedido>> tarea = new Task<>() {
            @Override
            protected Optional<Pedido> call()
                    throws SQLException {
                        System.out.println(">>> BUSCANDO PEDIDO ACTIVO");
                        return pedidoService.obtenerPedidoActivo(mesaId);
                    }
                };

        tarea.setOnSucceeded(e -> {Optional<Pedido> resultado = tarea.getValue();
            if (resultado.isEmpty()) {
                System.out.println(">>> NO HAY PEDIDO ACTIVO PARA LA MESA " + mesaId);
                return;
            }

            pedidoActual = resultado.get();
            System.out.println(">>> PEDIDO ACTIVO ENCONTRADO: " + pedidoActual.getNumeroPedido());
            System.out.println(">>> PRODUCTOS GUARDADOS: " + pedidoActual.getProductos());

            try {
                cargarProductosDesdeJson(pedidoActual.getProductos());
                actualizarPedido();
                System.out.println(">>> PEDIDO ACTIVO CARGADO EN LA INTERFAZ");
            } catch (Exception ex) {
                pedidoActivoCargado = false;
                System.out.println(">>> ERROR CARGANDO PRODUCTOS DEL PEDIDO");
                ex.printStackTrace();
                mostrarError(ex);
            }
        });

        tarea.setOnFailed(e -> {
            pedidoActivoCargado = false;
            Throwable error = tarea.getException();
            System.out.println(">>> ERROR BUSCANDO PEDIDO ACTIVO");
            if (error != null) {
                error.printStackTrace();
            }
            mostrarError(error);
        });

        iniciar(tarea, "cargar-pedido-activo");
    }

    /**
     * Convierte el JSON almacenado en pedidos.productos
     * nuevamente en ItemPedido.
     *
     * El JSON que manejamos actualmente tiene esta estructura:
     *
     * [
     *   {
     *     "producto_id":1,
     *     "cantidad":2,
     *     "precio_unitario":18000,
     *     "subtotal":36000
     *   }
     * ]
     */
    private void cargarProductosDesdeJson(String json) {

        pedido.clear();
        if (json == null || json.isBlank()) {
            return;
        }
        String contenido = json.trim();
        if (contenido.equals("[]")) {
            return;
        }
        /*
         * Busca cada objeto individual dentro del arreglo.
         */
        Pattern objetoPattern = Pattern.compile("\\{([^}]*)\\}");
        Matcher objetos = objetoPattern.matcher(contenido);
        while (objetos.find()) {
            String objeto = objetos.group(1);
            Long productoId = extraerLong(objeto, "\"producto_id\"\\s*:\\s*(\\d+)");
            Integer cantidad = extraerInteger(objeto, "\"cantidad\"\\s*:\\s*(\\d+)");
            if (productoId == null || cantidad == null || cantidad <= 0) {
                continue;
            }

            Producto producto = buscarProductoEnMenu(productoId);
            if (producto == null) {
                System.out.println(">>> PRODUCTO " + productoId + " NO ENCONTRADO EN EL MENU");
                continue;
            }

            ItemPedido item = new ItemPedido(producto, cantidad);
            pedido.put(productoId, item);
            System.out.println(">>> CARGADO: " + producto.getNombre() + " x " + cantidad);
        }
    }

    /**
     * Busca un producto por ID dentro del menú completo.
     */
    private Producto buscarProductoEnMenu(long productoId) {

        for (List<Producto> productos : menuCompleto.values()) {
            for (Producto producto : productos) {
                if (producto.getProductoId() == productoId) {
                    return producto;
                }
            }
        }
        return null;
    }

    private Long extraerLong(String texto, String expresion) {
        Pattern pattern = Pattern.compile(expresion);
        Matcher matcher = pattern.matcher(texto);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
    private Integer extraerInteger(String texto, String expresion) {
        Pattern pattern = Pattern.compile(expresion);
        Matcher matcher = pattern.matcher(texto);
        if (matcher.find()) {
            try {
                return Integer.parseInt(
                        matcher.group(1)
                );
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
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
            if (categoria != null && !TODAS_LAS_CATEGORIAS.equals(categoria) && !categoria.equals(entrada.getKey())) {
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

    /**
     * Agrega una unidad de un producto al pedido.
     */
    private void agregarProducto(Producto producto) {
        /*
         * Una vez descontado el inventario, no permitimos
         * modificar las cantidades del pedido.
         *
         * Esto evita que el pedido y el inventario queden
         * desincronizados.
         */
        if (pedidoActual != null && pedidoActual.isInventarioDescontado()) {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setTitle("Pedido confirmado");
            alerta.setHeaderText("No se puede modificar el pedido");
            alerta.setContentText("El inventario de este pedido ya fue descontado. " + "No puedes agregar más productos.");
            alerta.showAndWait();
            return;
        }
        try {
            ItemPedido existente = pedido.get(producto.getProductoId());
            int cantidadSolicitada = existente == null ? 1 : existente.getCantidad() + 1;
            if (!productoDAO.estaDisponible(producto.getProductoId(), cantidadSolicitada)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("No hay inventario suficiente");
                if (existente == null) {
                    alert.setContentText(producto.getNombre() + " ya no tiene ingredientes " + "suficientes y no puede agregarse.");
                    cargarMenu();
                } else {
                    alert.setContentText("No alcanza para " + cantidadSolicitada + " unidades de " + producto.getNombre() + ". Se mantienen las " + existente.getCantidad() + " que ya tenía en el pedido.");
                }
                alert.showAndWait();
                return;
            }
            if (existente == null) {pedido.put(producto.getProductoId(), new ItemPedido(producto));
            } else {existente.aumentarCantidad();
            }
            actualizarPedido();
        } catch (SQLException e) {
            mostrarError(e);
        }
    }

    /**
     * Actualiza visualmente el pedido actual.
     */
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
                    if (pedidoActual != null && pedidoActual.isInventarioDescontado()) {
                        Alert alerta = new Alert(Alert.AlertType.WARNING);
                        alerta.setTitle("Pedido confirmado");
                        alerta.setHeaderText("No se puede modificar el pedido");
                        alerta.setContentText("El inventario de este pedido ya fue descontado.");
                        alerta.showAndWait();
                        return;
                    }
                    if (item.getCantidad() == 1) {
                        pedido.remove(item.getProducto().getProductoId());
                    } else {
                        item.disminuirCantidad();
                    }
                    actualizarPedido();
                });

                eliminar.setOnAction(e -> {
                    if (pedidoActual != null && pedidoActual.isInventarioDescontado()) {
                        Alert alerta = new Alert(Alert.AlertType.WARNING);
                        alerta.setTitle("Pedido confirmado");
                        alerta.setHeaderText("No se puede modificar el pedido");
                        alerta.setContentText("El inventario de este pedido ya fue descontado.");
                        alerta.showAndWait();
                        return;
                    }
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
     * Valida y guarda el pedido completo.
     */
    @FXML
    private void confirmarPedido() {

        if (pedido.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Debe agregar al menos un producto.").showAndWait();
            return;
        }
        if (mesaId == null || usuarioId == null) {
            mostrarError(new IllegalStateException("El pedido no tiene una mesa " + "o usuario asociado."));
            return;
        }
        /*
         * Si el inventario ya fue descontado, no permitimos
         * volver a confirmar el pedido.
         *
         * Esto evita descontar dos veces los mismos ingredientes.
         */
        if (pedidoActual != null && pedidoActual.isInventarioDescontado()) {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setTitle("Inventario ya descontado");
            alerta.setHeaderText("Este pedido ya fue confirmado");
            alerta.setContentText("El inventario de este pedido ya fue descontado. " + "No se puede volver a confirmar para evitar " + "un descuento duplicado.");
            alerta.showAndWait();
            return;
        }

        Map<Long, Integer> lineas = new LinkedHashMap<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : pedido.values()) {lineas.put(item.getProducto().getProductoId(), item.getCantidad());subtotal = subtotal.add(item.getSubtotal());
        }
        final BigDecimal subtotalFinal = subtotal;
        Task<List<String>> tarea = new Task<>() {
            @Override
            protected List<String> call() throws SQLException {
                System.out.println(">>> INICIANDO VALIDACION " + "DE INVENTARIO");
                List<String> resultado = disponibilidadDAO.faltantesDelPedido(lineas);
                System.out.println(">>> VALIDACION TERMINADA: " + resultado);
                return resultado;
            }
        };
        tarea.setOnSucceeded(e -> {
            List<String> faltantes = tarea.getValue();
            /*
             * Primero mostramos los ingredientes/productos
             * que no tienen disponibilidad suficiente.
             */
            if (!faltantes.isEmpty()) {
                Alert alerta = new Alert(Alert.AlertType.WARNING);
                alerta.setHeaderText("El pedido completo " + "no se puede preparar");
                alerta.setContentText("Falta inventario para:\n\n• " + String.join("\n• ", faltantes));
                alerta.showAndWait();
                cargarMenu();
                return;
            }

            /*
             * Construimos el JSON completo del pedido.
             */
            StringBuilder json = new StringBuilder();
            json.append("[");
            boolean primero = true;
            for (ItemPedido item : pedido.values()) {
                if (!primero) {json.append(",");
                }
                json.append("{")
                        .append("\"producto_id\":")
                        .append(item.getProducto().getProductoId())
                        .append(",")
                        .append("\"cantidad\":")
                        .append(item.getCantidad())
                        .append(",")
                        .append("\"precio_unitario\":")
                        .append(item.getProducto().getPrecioVenta())
                        .append(",")
                        .append("\"subtotal\":")
                        .append(item.getSubtotal())
                        .append("}");
                primero = false;
            }
            json.append("]");
            /*
             * Indica si tuvimos que crear un pedido nuevo.
             *
             * Si el descuento falla en un pedido recién creado,
             * podremos cancelarlo para no dejar una comanda vacía
             * ocupando la mesa.
             */
            boolean pedidoNuevo = pedidoActual == null || pedidoActual.getId() == null;
            try {
                /*
                 * Busca el pedido activo.
                 *
                 * Si ya existe, utiliza ese mismo pedido.
                 * Si no existe, crea uno nuevo.
                 */
                pedidoActual = pedidoService.obtenerOCrearPedido(mesaId, usuarioId);
                /*
                 * Seguridad adicional:
                 * volvemos a comprobar que el inventario
                 * no haya sido descontado.
                 */
                if (pedidoActual.isInventarioDescontado()) {
                    Alert alerta = new Alert(Alert.AlertType.WARNING);
                    alerta.setTitle("Pedido ya confirmado");
                    alerta.setHeaderText("El inventario ya fue descontado");
                    alerta.setContentText("Este pedido ya tiene registrado " + "el descuento de inventario.");
                    alerta.showAndWait();
                    return;
                }
                /*
                 * Guardamos los datos actuales del pedido
                 * antes de descontar el inventario.
                 */
                pedidoActual.setProductos(json.toString());
                pedidoActual.setSubtotal(subtotalFinal);
                pedidoActual.setTotal(subtotalFinal);
                /*
                 * Primero actualizamos el pedido para asegurarnos
                 * de que existe y tiene su información completa.
                 */
                pedidoService.actualizarPedido(pedidoActual);
                /*
                 * AHORA SE REALIZA EL DESCUENTO REAL DEL INVENTARIO.
                 *
                 * InventarioService:
                 * - obtiene las recetas
                 * - calcula cantidades
                 * - bloquea los ingredientes
                 * - verifica stock
                 * - descuenta stock
                 * - registra movimientos SALIDA
                 */
                System.out.println(">>> INICIANDO DESCUENTO REAL DE INVENTARIO");
                inventarioService.descontarIngredientes(pedidoActual.getId(), lineas, usuarioId);
                System.out.println(">>> DESCUENTO DE INVENTARIO COMPLETADO");
                /*
                 * Marcamos el pedido para indicar que sus
                 * ingredientes ya fueron descontados.
                 */
                pedidoActual.setInventarioDescontado(true);
                /*
                 * Guardamos nuevamente el pedido con:
                 *
                 * inventario_descontado = '1'
                 */
                pedidoService.actualizarPedido(pedidoActual);
                Alert alerta = new Alert(Alert.AlertType.INFORMATION);
                alerta.setTitle("Pedido actualizado");
                alerta.setHeaderText("Pedido registrado correctamente");
                alerta.setContentText("Pedido: "
                                + pedidoActual.getNumeroPedido()
                                + "\nMesa: "
                                + mesaId
                                + "\nProductos: "
                                + pedido.size()
                                + "\nSubtotal: "
                                + moneda.format(subtotalFinal)
                                + "\nTotal: "
                                + moneda.format(subtotalFinal)
                                + "\n\nInventario descontado correctamente.");
                alerta.showAndWait();
                /*
                 * Actualizamos la interfaz para que los controles
                 * de modificación queden bloqueados.
                 */
                actualizarPedido();
            } catch (Exception ex) {
                /*
                 * Si acabábamos de crear el pedido y el descuento
                 * falla, cancelamos ese pedido para no dejar
                 * una comanda vacía ocupando la mesa.
                 */
                if (pedidoNuevo && pedidoActual != null && pedidoActual.getId() != null) {
                    try {pedidoService.cancelarPedido(pedidoActual.getId(), "DISPONIBLE");
                        pedidoActual = null;
                    } catch (Exception errorCancelacion) {
                        System.err.println(">>> NO SE PUDO CANCELAR " + "EL PEDIDO CREADO: " + errorCancelacion.getMessage());
                    }
                }
                mostrarError(ex);
            }
        });
        tarea.setOnFailed(e -> mostrarError(tarea.getException()));
        iniciar(tarea, "validar-pedido");
    }
    private void iniciar(Task<?> tarea, String nombreHilo) {
        Thread hilo = new Thread(tarea, nombreHilo);
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML
    private void anularPedido() {
        if (pedidoActual == null || pedidoActual.getId() == null) {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setTitle("Anular pedido");
            alerta.setHeaderText("No hay un pedido guardado");
            alerta.setContentText("Primero debes tener un pedido confirmado para poder anularlo.");
            alerta.showAndWait();
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Anular pedido");
        confirmacion.setHeaderText("¿Deseas anular este pedido?");
        confirmacion.setContentText("El pedido se marcará como cancelado y la mesa quedará disponible.");
        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isEmpty() || resultado.get() != ButtonType.OK) {return;
        }
        try {
            pedidoService.cancelarPedido(pedidoActual.getId(), "DISPONIBLE");
            pedidoActual = null;
            pedido.clear();
            actualizarPedido();
            Alert exito = new Alert(Alert.AlertType.INFORMATION);
            exito.setTitle("Pedido anulado");
            exito.setHeaderText("Pedido anulado correctamente");
            exito.setContentText("El pedido fue cancelado y la mesa quedó disponible.");
            exito.showAndWait();
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }
    @FXML
    private void cerrarPedido() {if (pedidoActual == null || pedidoActual.getId() == null) {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setTitle("Cerrar pedido");
            alerta.setHeaderText("No hay un pedido guardado");
            alerta.setContentText("Confirma el pedido antes de intentar cerrarlo.");
            alerta.showAndWait();
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Cerrar pedido");
        confirmacion.setHeaderText("¿Deseas cerrar este pedido?");
        confirmacion.setContentText("El pedido se marcará como completado y se liberará la mesa.");
        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isEmpty() || resultado.get() != ButtonType.OK) {return;
        }
        try {
            pedidoService.cerrarPedido(pedidoActual.getId(), "DISPONIBLE");
            pedidoActual = null;
            pedido.clear();
            actualizarPedido();
            Alert exito = new Alert(Alert.AlertType.INFORMATION);
            exito.setTitle("Pedido cerrado");
            exito.setHeaderText("Pedido cerrado correctamente");
            exito.setContentText("El pedido se cerró y se solicitó liberar la mesa.");
            exito.showAndWait();
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }


    private void mostrarError(Throwable e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Error de base de datos");
        alert.setContentText(e == null ? "Error desconocido" : e.getMessage());
        alert.showAndWait();
    }
}