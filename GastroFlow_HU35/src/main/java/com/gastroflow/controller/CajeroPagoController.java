package com.gastroflow.controller;

import com.gastroflow.dao.PagoDAO;
import com.gastroflow.model.CuentaPago;
import com.gastroflow.session.SesionUsuario;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;

public class CajeroPagoController {

    @FXML private TextField numeroPedidoField;
    @FXML private Label cajeroLabel;
    @FXML private Label estadoCuentaLabel;
    @FXML private Label pedidoLabel;
    @FXML private Label mesaLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label impuestosLabel;
    @FXML private Label subtotalResumenLabel;
    @FXML private Label impuestosResumenLabel;
    @FXML private TextField descuentoField;
    @FXML private TextField motivoDescuentoField;
    @FXML private TextField propinaField;
    @FXML private Label descuentoResumenLabel;
    @FXML private Label propinaResumenLabel;
    @FXML private Label totalLabel;
    @FXML private Button buscarPedidoButton;
    @FXML private Button eliminarDescuentoButton;
    @FXML private Button eliminarPropinaButton;
    @FXML private Button cerrarPagoButton;

    private final PagoDAO pagoDAO = new PagoDAO();
    private final NumberFormat moneda = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    private CuentaPago cuentaActual;

    @FXML
    public void initialize() {
        limpiarCuenta();

        descuentoField.textProperty().addListener((obs, anterior, nuevo) -> recalcular());
        propinaField.textProperty().addListener((obs, anterior, nuevo) -> recalcular());
        motivoDescuentoField.textProperty().addListener((obs, anterior, nuevo) -> recalcular());

        if (!SesionUsuario.estaAutenticado() || !SesionUsuario.esCajero()) {
            cajeroLabel.setText("Sin sesión de Cajero");
            bloquearPantalla(true);
            Platform.runLater(() -> mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Acceso restringido",
                    "Debe iniciar sesión con el rol Cajero para gestionar pagos."
            ));
        } else {
            cajeroLabel.setText(SesionUsuario.getNombre());
            bloquearPantalla(false);
        }
    }

    @FXML
    private void buscarPedido() {
        String numeroPedido = numeroPedidoField.getText() == null
                ? ""
                : numeroPedidoField.getText().trim();

        if (numeroPedido.isBlank()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Pedido requerido", "Ingrese el número del pedido.");
            return;
        }

        try {
            var resultado = pagoDAO.buscarCuenta(numeroPedido);

            if (resultado.isEmpty()) {
                limpiarCuenta();
                mostrarAlerta(Alert.AlertType.WARNING, "Pedido no encontrado", "No se encontró una cuenta activa con ese número de pedido.");
                return;
            }

            cuentaActual = resultado.get();
            mostrarCuenta();

        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de base de datos", e.getMessage());
        }
    }

    private void mostrarCuenta() {
        pedidoLabel.setText(cuentaActual.getNumeroPedido());
        mesaLabel.setText(cuentaActual.getNumeroMesa() == null ? "Sin mesa" : "Mesa " + cuentaActual.getNumeroMesa());
        subtotalLabel.setText(moneda.format(cuentaActual.getSubtotal()));
        impuestosLabel.setText(moneda.format(cuentaActual.getImpuestos()));
        subtotalResumenLabel.setText(moneda.format(cuentaActual.getSubtotal()));
        impuestosResumenLabel.setText(moneda.format(cuentaActual.getImpuestos()));
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

        BigDecimal descuento = leerValor(descuentoField.getText());
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
                    SesionUsuario.getUsuarioId(),
                    motivoDescuentoField.getText()
            );

            mostrarAlerta(
                    Alert.AlertType.INFORMATION,
                    "Pago registrado",
                    "El pago se cerró correctamente por " + moneda.format(cuentaActual.calcularTotal()) + "."
            );

            numeroPedidoField.clear();
            limpiarCuenta();

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
        descuentoField.setText("0.00");
        motivoDescuentoField.clear();
        propinaField.setText("0.00");
        descuentoResumenLabel.setText(moneda.format(BigDecimal.ZERO));
        propinaResumenLabel.setText(moneda.format(BigDecimal.ZERO));
        totalLabel.setText(moneda.format(BigDecimal.ZERO));
        setEdicionCuenta(false);
    }

    private void setEdicionCuenta(boolean habilitada) {
        descuentoField.setDisable(!habilitada);
        motivoDescuentoField.setDisable(!habilitada);
        propinaField.setDisable(!habilitada);
        eliminarDescuentoButton.setDisable(!habilitada);
        eliminarPropinaButton.setDisable(!habilitada);
        cerrarPagoButton.setDisable(!habilitada);
    }

    private void bloquearPantalla(boolean bloqueada) {
        numeroPedidoField.setDisable(bloqueada);
        buscarPedidoButton.setDisable(bloqueada);
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
