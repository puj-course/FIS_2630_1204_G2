package com.restaurante.controller;

import entity.Reserva;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import repository.ReservaRepository;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReservaMeseroController {

    @FXML
    private TableView<Reserva> tablaReservas;

    @FXML
    private TableColumn<Reserva, String> colMesa;

    @FXML
    private TableColumn<Reserva, String> colZona;

    @FXML
    private TableColumn<Reserva, String> colHora;

    @FXML
    private TableColumn<Reserva, String> colCliente;

    @FXML
    private TableColumn<Reserva, Integer> colPersonas;

    private final ReservaRepository reservaRepository = new ReservaRepository();

    private static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        colMesa.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(
                        "Mesa " + data.getValue().getNumeroMesa()
                )
        );
        colZona.setCellValueFactory(new PropertyValueFactory<>("nombreZona"));
        colHora.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getFechaHoraReserva().format(FORMATO_HORA)
                )
        );
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
        colPersonas.setCellValueFactory(new PropertyValueFactory<>("cantidadPersonas"));

        cargarTodasLasReservas();
    }

    @FXML
    private void cargarTodasLasReservas() {
        try {
            List<Reserva> reservas = reservaRepository.obtenerMesasReservadas();
            ObservableList<Reserva> datos = FXCollections.observableArrayList(reservas);
            tablaReservas.setItems(datos);

        } catch (SQLException e) {
            mostrarError("Error al cargar mesas reservadas: " + e.getMessage());
        }
    }

    @FXML
    private void filtrarTurnoActual() {
        try {
            List<Reserva> reservas = reservaRepository.obtenerReservasDelTurnoActual();
            ObservableList<Reserva> datos = FXCollections.observableArrayList(reservas);
            tablaReservas.setItems(datos);

        } catch (SQLException e) {
            mostrarError("Error al filtrar reservas del turno actual: " + e.getMessage());
        }
    }

    @FXML
    private void archivarReservaSeleccionada() {
        Reserva seleccionada = tablaReservas.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {
            mostrarError("Seleccione una reserva primero.");
            return;
        }

        try {
            reservaRepository.archivarReserva(seleccionada.getIdReserva());
            cargarTodasLasReservas();

        } catch (SQLException e) {
            mostrarError("Error al archivar la reserva: " + e.getMessage());
        }
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
