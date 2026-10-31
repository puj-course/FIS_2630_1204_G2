package controller;

import dao.PagoDAO;
import dto.CuentaPago;
import service.descuento.CalculadoraDescuento;
import service.descuento.DescuentoMontoFijo;
import service.descuento.DescuentoPorcentaje;
import service.descuento.DescuentoStrategy;
import session.SesionUsuario;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CajeroPagoController {

    @FXML private ComboBox<CuentaPago> cuentaCombo;
    @FXML private Label cajeroLabel;
    @FXML private Label estadoCuentaLabel;
    @FXML private Label pedidoLabel;
    @FXML private Label mesaLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label impuestosLabel;
    @FXML private Label subtotalResumenLabel;
    @FXML private ComboBox<DescuentoStrategy> tipoDescuentoCombo;
    @FXML private Label impuestosResumenLabel;
    @FXML private TextField descuentoField;
    @FXML private TextField motivoDescuentoField;
    @FXML private TextField propinaField;
    @FXML private Label descuentoResumenLabel;
    @FXML private Label propinaResumenLabel;
    @FXML private Label totalLabel;
    @FXML private Button recargarButton;
    @FXML private Button eliminarDescuentoButton;
    @FXML private Button eliminarPropinaButton;
    @FXML private Button cerrarPagoButton;

    private final PagoDAO pagoDAO = new PagoDAO();
    private final CalculadoraDescuento calculadoraDescuento = new CalculadoraDescuento();
    private final NumberFormat moneda = NumberFormat.getCurrencyInstance(Locale.of("es", "CO"));

    private CuentaPago cuentaActual;

    @FXML
    public void initialize() {
        // se implementa el patrón Strategy para calcular las formas en que se calcula y el cajero pueda elegir

        // HU-144: la lista muestra algo que el cajero pueda reconocer de un
        // vistazo (mesa, numero y total), no el codigo a secas.
        cuentaCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(CuentaPago cuenta) {
                if (cuenta == null) {
                    return "";
                }
                String mesa = cuenta.getNumeroMesa() == null
                        ? "Sin mesa"
                        : "Mesa " + cuenta.getNumeroMesa();
                return mesa + " · " + cuenta.getNumeroPedido()
                        + " · " + moneda.format(cuenta.calcularTotal())
                        + (cuenta.isPagada() ? " (pagada)" : "");
            }

            @Override
            public CuentaPago fromString(String texto) {
                return null;   // la lista no es editable
            }
        });
        cuentaCombo.valueProperty().addListener((obs, anterior, seleccionada) -> {
            if (seleccionada == null) {
                return;        // lo dispara recargar la lista, no el cajero
            }
            cuentaActual = seleccionada;
            mostrarCuenta();
        });

        tipoDescuentoCombo.getItems().addAll(new DescuentoMontoFijo(), new DescuentoPorcentaje());
        tipoDescuentoCombo.getSelectionModel().selectFirst();
        tipoDescuentoCombo.valueProperty().addListener((obs, anterior, nuevo) -> {
            calculadoraDescuento.setEstrategia(nuevo);
            recalcular();
        });
        limpiarCuenta();

        descuentoField.textProperty().addListener((obs, anterior, nuevo) -> recalcular());
        propinaField.textProperty().addListener((obs, anterior, nuevo) -> recalcular());
        motivoDescuentoField.textProperty().addListener((obs, anterior, nuevo) -> recalcular());

        if (!SesionUsuario.getInstance().estaAutenticado() || !SesionUsuario.getInstance().esCajero()) { //cambio a instance por singleton
            cajeroLabel.setText("Sin sesión de Cajero");
            bloquearPantalla(true);
            Platform.runLater(() -> mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Acceso restringido",
                    "Debe iniciar sesión con el rol Cajero para gestionar pagos."
            ));
        } else {
            cajeroLabel.setText(SesionUsuario.getInstance().getNombre()); //cambio por singleton inicio sesión
            bloquearPantalla(false);
            recargarCuentas();
        }
    }

    /**
     * HU-144: recarga las cuentas de la lista desplegable.
     *
     * Se llama al abrir la pantalla, con el boton "Actualizar" y despues de
     * cerrar un pago, para que la cuenta recien cobrada aparezca ya marcada
     * como pagada.
     */
    @FXML
    private void recargarCuentas() {
        try {
            List<CuentaPago> cuentas = pagoDAO.listarCuentas();
            cuentaCombo.getItems().setAll(cuentas);
            cuentaCombo.getSelectionModel().clearSelection();
            limpiarCuenta();

            if (cuentas.isEmpty()) {
                cuentaCombo.setPromptText("No hay cuentas para cobrar");
            } else {
                cuentaCombo.setPromptText("Seleccione un pedido");
            }

        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudieron cargar las cuentas",
                    "No se pudo consultar la lista de pedidos. " + e.getMessage());
        }
    }

    private void mostrarCuenta() {
        pedidoLabel.setText(cuentaActual.getNumeroPedido());
        mesaLabel.setText(cuentaActual.getNumeroMesa() == null ? "Sin mesa" : "Mesa " + cuentaActual.getNumeroMesa());
        subtotalLabel.setText(moneda.format(cuentaActual.getSubtotal()));
        impuestosLabel.setText(moneda.format(cuentaActual.getImpuestos()));
        subtotalResumenLabel.setText(moneda.format(cuentaActual.getSubtotal()));
        impuestosResumenLabel.setText(moneda.format(cuentaActual.getImpuestos()));
        // El descuento guardado ya está en pesos: se muestra como monto fijo.
        tipoDescuentoCombo.getSelectionModel().selectFirst();
        descuentoField.setText(cuentaActual.getDescuento().setScale(2, RoundingMode.HALF_UP).toPlainString());
        propinaField.setText(cuentaActual.getPropina().setScale(2, RoundingMode.HALF_UP).toPlainString());
        motivoDescuentoField.clear();

        if (cuentaActual.isPagada()) {
            estadoCuentaLabel.setText("PAGADA");
            setEdicionCuenta(false);
        } else {
            estadoCuentaLabel.setText("PENDIENTE DE PAGO");
            setEdicionCuenta(true);
        }

        recalcular();
    }

    private void recalcular() {
        if (cuentaActual == null) {
            subtotalResumenLabel.setText(moneda.format(BigDecimal.ZERO));
            impuestosResumenLabel.setText(moneda.format(BigDecimal.ZERO));
            descuentoResumenLabel.setText(moneda.format(BigDecimal.ZERO));
            propinaResumenLabel.setText(moneda.format(BigDecimal.ZERO));
            totalLabel.setText(moneda.format(BigDecimal.ZERO));
            return;
        }

        BigDecimal valorDescuento = leerValor(descuentoField.getText());
        // El valor escrito se convierte a pesos con la estrategia elegida.
        BigDecimal descuento = valorDescuento == null
                ? null
                : tipoDescuentoCombo.getValue().calcular(cuentaActual.getSubtotal(), valorDescuento);
        BigDecimal propina = leerValor(propinaField.getText());

        if (descuento == null || propina == null) {
            totalLabel.setText("Valor inválido");
            cerrarPagoButton.setDisable(true);
            return;
        }

        if (descuento.compareTo(cuentaActual.getSubtotal()) > 0) {
            totalLabel.setText("Descuento demasiado alto");
            cerrarPagoButton.setDisable(true);
            return;
        }

        if (descuento.compareTo(BigDecimal.ZERO) > 0
                && (motivoDescuentoField.getText() == null || motivoDescuentoField.getText().isBlank())) {
            cuentaActual.setDescuento(descuento);
            cuentaActual.setPropina(propina);
            actualizarResumen();
            cerrarPagoButton.setDisable(true);
            return;
        }

        cuentaActual.setDescuento(descuento);
        cuentaActual.setPropina(propina);
        actualizarResumen();

        if (cuentaActual.calcularTotal().compareTo(BigDecimal.ZERO) < 0) {
            totalLabel.setText("Total inválido");
            cerrarPagoButton.setDisable(true);
            return;
        }

        cerrarPagoButton.setDisable(cuentaActual.isPagada());
    }

    private void actualizarResumen() {
        descuentoResumenLabel.setText("- " + moneda.format(cuentaActual.getDescuento()));
        propinaResumenLabel.setText("+ " + moneda.format(cuentaActual.getPropina()));
        totalLabel.setText(moneda.format(cuentaActual.calcularTotal()));
    }

    @FXML
    private void eliminarPropina() {
        propinaField.setText("0.00");
    }

    @FXML
    private void eliminarDescuento() {
        tipoDescuentoCombo.getSelectionModel().selectFirst();
        descuentoField.setText("0.00");
        motivoDescuentoField.clear();
    }

    @FXML
    private void cerrarPago() {
        if (cuentaActual == null || cuentaActual.isPagada()) {
            return;
        }

        recalcular();

        if (cerrarPagoButton.isDisable()) {
            mostrarAlerta(
                    Alert.AlertType.WARNING,
                    "Datos pendientes",
                    "Revise el descuento, su motivo y la propina antes de cerrar el pago."
            );
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar cierre");
        confirmacion.setHeaderText("Total a pagar: " + moneda.format(cuentaActual.calcularTotal()));
        confirmacion.setContentText("Después de cerrar el pago, la cuenta quedará bloqueada para nuevos pagos.");

        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            pagoDAO.cerrarPago(
                    cuentaActual,
                    SesionUsuario.getInstance().getUsuarioId(), //cambio por singleton iniciar sesión
                    motivoDescuentoField.getText()
            );

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Pago registrado",
                    "El pago se cerró correctamente por " + moneda.format(cuentaActual.calcularTotal()) + "."
            );

            recargarCuentas();

        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "No se pudo cerrar el pago", e.getMessage());
        }
    }

    private BigDecimal leerValor(String texto) {
        if (texto == null || texto.isBlank()) {
            return BigDecimal.ZERO;
        }

        try {
            String normalizado = texto.trim().replace("$", "").replace(" ", "");

            if (normalizado.contains(",") && normalizado.contains(".")) {
                normalizado = normalizado.replace(".", "").replace(",", ".");
            } else if (normalizado.contains(",")) {
                normalizado = normalizado.replace(",", ".");
            }

            if (normalizado.matches(".*[eE].*")) {
                return null;
            }

            BigDecimal valor = new BigDecimal(normalizado);
            return valor.signum() < 0 ? null : valor.setScale(2, RoundingMode.HALF_UP);

        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void limpiarCuenta() {
        cuentaActual = null;
        estadoCuentaLabel.setText("-");
        pedidoLabel.setText("-");
        mesaLabel.setText("-");
        subtotalLabel.setText(moneda.format(BigDecimal.ZERO));
        impuestosLabel.setText(moneda.format(BigDecimal.ZERO));
        subtotalResumenLabel.setText(moneda.format(BigDecimal.ZERO));
        impuestosResumenLabel.setText(moneda.format(BigDecimal.ZERO));
        tipoDescuentoCombo.getSelectionModel().selectFirst();
        descuentoField.setText("0.00");
        motivoDescuentoField.clear();
        propinaField.setText("0.00");
        descuentoResumenLabel.setText(moneda.format(BigDecimal.ZERO));
        propinaResumenLabel.setText(moneda.format(BigDecimal.ZERO));
        totalLabel.setText(moneda.format(BigDecimal.ZERO));
        setEdicionCuenta(false);
    }

    private void setEdicionCuenta(boolean habilitada) {
        tipoDescuentoCombo.setDisable(!habilitada);
        descuentoField.setDisable(!habilitada);
        motivoDescuentoField.setDisable(!habilitada);
        propinaField.setDisable(!habilitada);
        eliminarDescuentoButton.setDisable(!habilitada);
        eliminarPropinaButton.setDisable(!habilitada);
        cerrarPagoButton.setDisable(!habilitada);
    }

    private void bloquearPantalla(boolean bloqueada) {
        cuentaCombo.setDisable(bloqueada);
        recargarButton.setDisable(bloqueada);
        if (bloqueada) {
            setEdicionCuenta(false);
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
