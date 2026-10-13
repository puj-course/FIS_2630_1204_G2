package controller;

import enums.EstadoPedido;
import entity.Mesa;
import entity.Pedido;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import repository.MesaRepository;
import repository.PedidoRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class CocinaController {

    @FXML private VBox columnaPendientes;
    @FXML private VBox columnaPreparacion;
    @FXML private VBox columnaListos;

    @FXML private Label lblPendientes;
    @FXML private Label lblPreparacion;
    @FXML private Label lblListos;

    private final PedidoRepository pedidoRepository = new PedidoRepository();
    private final MesaRepository mesaRepository = new MesaRepository();

    // Umbrales de alerta por tiempo de espera, en minutos
    private static final long MINUTOS_ALERTA_MEDIA = 10;
    private static final long MINUTOS_ALERTA_ALTA = 20;

    @FXML
    public void initialize() {
        cargarTablero();

        // Refresca cada 30s: recarga la lista y recalcula los tiempos/colores de alerta
        Timeline refresco = new Timeline(new KeyFrame(Duration.seconds(30), e -> cargarTablero()));
        refresco.setCycleCount(Animation.INDEFINITE);
        refresco.play();
    }

    private void cargarTablero() {
        try {
            List<Pedido> pedidos = pedidoRepository.findPedidosParaCocina();

            columnaPendientes.getChildren().clear();
            columnaPreparacion.getChildren().clear();
            columnaListos.getChildren().clear();

            int pendientes = 0, preparacion = 0, listos = 0;

            for (Pedido pedido : pedidos) {
                VBox tarjeta = crearTarjeta(pedido);

                switch (pedido.getEstado()) {
                    case EN_PREPARACION -> {
                        columnaPreparacion.getChildren().add(tarjeta);
                        preparacion++;
                    }
                    case COMPLETADO -> {
                        columnaListos.getChildren().add(tarjeta);
                        listos++;
                    }
                    default -> {
                        // PENDIENTE y ASIGNADO_MESA se agrupan visualmente como "pendientes"
                        columnaPendientes.getChildren().add(tarjeta);
                        pendientes++;
                    }
                }
            }

            lblPendientes.setText("Pendientes (" + pendientes + ")");
            lblPreparacion.setText("En preparación (" + preparacion + ")");
            lblListos.setText("Listos (" + listos + ")");

        } catch (SQLException e) {
            mostrarError("Error al cargar el tablero de cocina: " + e.getMessage());
        }
    }

    private VBox crearTarjeta(Pedido pedido) {
        VBox tarjeta = new VBox(8);
        tarjeta.getStyleClass().add("comanda-card");

        String numeroMesaTexto = "Mesa -";
        try {
            Mesa mesa = mesaRepository.findById(pedido.getMesaId().intValue()).orElse(null);
            if (mesa != null) {
                numeroMesaTexto = "Mesa " + mesa.getNumeroMesa();
            }
        } catch (SQLException ignored) {
            // Si falla la consulta de la mesa, se muestra "Mesa -" en vez de romper el tablero
        }

        Label numeroPedido = new Label(pedido.getNumeroPedido());
        numeroPedido.getStyleClass().add("comanda-numero");

        Label mesaLabel = new Label(numeroMesaTexto);
        mesaLabel.getStyleClass().add("comanda-mesa");

        long minutosEspera = ChronoUnit.MINUTES.between(pedido.getFechaPedido(), LocalDateTime.now());
        Label tiempoLabel = new Label(formatearTiempo(minutosEspera));
        tiempoLabel.getStyleClass().add("comanda-tiempo");
        tiempoLabel.getStyleClass().add(claseAlertaPorTiempo(minutosEspera));

        tarjeta.getChildren().addAll(numeroPedido, mesaLabel, tiempoLabel);
        tarjeta.setAlignment(Pos.CENTER_LEFT);

        // Un pedido ya COMPLETADO no tiene siguiente paso en cocina: no se le agrega botón
        if (pedido.getEstado() != EstadoPedido.COMPLETADO) {
            Button avanzarBtn = new Button(textoSiguienteEstado(pedido.getEstado()));
            avanzarBtn.getStyleClass().add("comanda-avanzar");
            avanzarBtn.setMaxWidth(Double.MAX_VALUE);
            avanzarBtn.setOnAction(e -> avanzarEstado(pedido));
            tarjeta.getChildren().add(avanzarBtn);
        }

        return tarjeta;
    }

    private String textoSiguienteEstado(EstadoPedido estado) {
        return switch (estado) {
            case PENDIENTE, ASIGNADO_MESA -> "Pasar a preparación";
            case EN_PREPARACION -> "Marcar como listo";
            default -> "—";
        };
    }

    private void avanzarEstado(Pedido pedido) {
        try {
            if (pedido.getEstado() == EstadoPedido.EN_PREPARACION) {
                pedido.setEstado(EstadoPedido.COMPLETADO);
            } else {
                pedido.setEstado(EstadoPedido.EN_PREPARACION);
            }

            pedidoRepository.save(pedido);
            cargarTablero();

        } catch (SQLException e) {
            mostrarError("Error al actualizar el pedido: " + e.getMessage());
        }
    }

    private String formatearTiempo(long minutos) {
        return minutos + " min";
    }

    // Codificación de color por tiempo de espera: normal / media / alta
    private String claseAlertaPorTiempo(long minutos) {
        if (minutos >= MINUTOS_ALERTA_ALTA) {
            return "alerta-alta";
        }
        if (minutos >= MINUTOS_ALERTA_MEDIA) {
            return "alerta-media";
        }
        return "alerta-normal";
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}