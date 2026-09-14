package com.restaurante.service;

import com.restaurante.entity.DetallePedido;
import com.restaurante.entity.Mesero;
import com.restaurante.enums.EstadoMesa;
import com.restaurante.entity.Mesa;
import com.restaurante.entity.Pedido;
import com.restaurante.enums.EstadoPedido;
import com.restaurante.repository.DetallePedidoRepository;
import com.restaurante.repository.MesaRepository;
import com.restaurante.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MesaService {
    private static final long TIEMPO_MAXIMO_MINUTOS = 60;
    private final MesaRepository mesaRepository;
    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;

    public MesaService(
            MesaRepository mesaRepository,
            PedidoRepository pedidoRepository, DetallePedidoRepository detallePedidoRepository
    ) {
        this.mesaRepository = mesaRepository;
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
    }

    @Transactional
    public void reasignarMesa(Long pedidoId, Long nuevaMesaId) {

        // 1. Buscar el pedido
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Pedido no encontrado con ID: " + pedidoId
                        )
                );

        // 2. Validar que el pedido pueda ser reasignado
        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new RuntimeException(
                    "No se puede reasignar la mesa de un pedido cancelado"
            );
        }

        // 3. Buscar la nueva mesa
        Mesa nuevaMesa = mesaRepository.findById(nuevaMesaId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Mesa no encontrada con ID: " + nuevaMesaId
                        )
                );

        // 4. Verificar que la nueva mesa esté disponible
        if (nuevaMesa.getEstado() != EstadoMesa.DISPONIBLE) {
            throw new RuntimeException(
                    "La mesa " + nuevaMesa.getNumero()
                            + " no está disponible"
            );
        }

        // 5. Obtener la mesa actual
        Mesa mesaActual = pedido.getMesa();

        // 6. Si el pedido ya tiene una mesa,
        //    liberar la mesa anterior
        if (mesaActual != null) {
            mesaActual.setEstado(EstadoMesa.DISPONIBLE);
            mesaRepository.save(mesaActual);
        }

        // 7. Ocupar la nueva mesa
        nuevaMesa.setEstado(EstadoMesa.OCUPADA);
        mesaRepository.save(nuevaMesa);

        // 8. Asociar el pedido con la nueva mesa
        pedido.setMesa(nuevaMesa);
        pedidoRepository.save(pedido);
    }
    @Transactional(readOnly = true)
    public String verificarDemora(Long mesaId) {

        Mesa mesa = mesaRepository.findById(mesaId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Mesa no encontrada con ID: " + mesaId
                        )
                );

        // Si la mesa está disponible, no existe demora
        if (mesa.getEstado() != EstadoMesa.OCUPADA) {
            return "La mesa " + mesa.getNumero()
                    + " está disponible. No hay alerta de demora.";
        }

        // Buscar el pedido activo de la mesa
        Pedido pedido = pedidoRepository
                .findByMesaAndEstado(
                        mesa,
                        EstadoPedido.PENDIENTE
                )
                .orElse(null);

        // Si no hay pedido pendiente, buscar uno confirmado
        if (pedido == null) {
            pedido = pedidoRepository
                    .findByMesaAndEstado(
                            mesa,
                            EstadoPedido.CONFIRMADO
                    )
                    .orElse(null);
        }

        if (pedido == null) {
            return "La mesa " + mesa.getNumero()
                    + " está ocupada, pero no tiene un pedido activo.";
        }

        // Calcular cuánto tiempo lleva el pedido activo
        LocalDateTime ahora = LocalDateTime.now();

        long minutosOcupada = Duration.between(
                pedido.getCreatedAt(),
                ahora
        ).toMinutes();

        // Verificar si supera el límite
        if (minutosOcupada >= TIEMPO_MAXIMO_MINUTOS) {

            return "ALERTA: La mesa " + mesa.getNumero()
                    + " lleva " + minutosOcupada
                    + " minutos ocupada.";
        }

        return "La mesa " + mesa.getNumero()
                + " lleva " + minutosOcupada
                + " minutos ocupada. No hay alerta.";
    }

    // Revisar todas las mesas ocupadas
    @Transactional(readOnly = true)
    public String verificarTodasLasDemoras() {

        List<Mesa> mesas = mesaRepository.findByEstado(
                EstadoMesa.OCUPADA
        );

        if (mesas.isEmpty()) {
            return "No hay mesas ocupadas.";
        }

        StringBuilder resultado = new StringBuilder();

        for (Mesa mesa : mesas) {

            Pedido pedido = pedidoRepository
                    .findByMesaAndEstado(
                            mesa,
                            EstadoPedido.PENDIENTE
                    )
                    .orElse(null);

            if (pedido == null) {
                pedido = pedidoRepository
                        .findByMesaAndEstado(
                                mesa,
                                EstadoPedido.CONFIRMADO
                        )
                        .orElse(null);
            }

            if (pedido == null) {
                continue;
            }

            long minutos = Duration.between(
                    pedido.getCreatedAt(),
                    LocalDateTime.now()
            ).toMinutes();

            if (minutos >= TIEMPO_MAXIMO_MINUTOS) {

                resultado.append(
                        "ALERTA: Mesa "
                );

                resultado.append(mesa.getNumero());

                resultado.append(
                        " lleva "
                );

                resultado.append(minutos);

                resultado.append(
                        " minutos ocupada.\n"
                );
            }
        }

        if (resultado.length() == 0) {
            return "No hay mesas con demora.";
        }

        return resultado.toString();
    }
    @Transactional(readOnly = true)
    public String consultarPedidoDeMesa(Long mesaId) {

        // Buscar la mesa
        Mesa mesa = mesaRepository.findById(mesaId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Mesa no encontrada con ID: " + mesaId
                        )
                );

        // Verificar que la mesa esté ocupada
        if (mesa.getEstado() != EstadoMesa.OCUPADA) {
            return "La mesa " + mesa.getNumero()
                    + " no está ocupada.";
        }

        // Buscar pedido pendiente
        Pedido pedido = pedidoRepository
                .findByMesaAndEstado(
                        mesa,
                        EstadoPedido.PENDIENTE
                )
                .orElse(null);

        // Si no existe, buscar pedido confirmado
        if (pedido == null) {
            pedido = pedidoRepository
                    .findByMesaAndEstado(
                            mesa,
                            EstadoPedido.CONFIRMADO
                    )
                    .orElse(null);
        }

        // No existe pedido activo
        if (pedido == null) {
            return "La mesa " + mesa.getNumero()
                    + " está ocupada pero no tiene un pedido activo.";
        }

        // Obtener los detalles del pedido
        List<DetallePedido> detalles =
                detallePedidoRepository.findByPedidoId(
                        pedido.getId()
                );

        StringBuilder resultado = new StringBuilder();

        resultado.append("===== PEDIDO DE LA MESA =====\n");
        resultado.append("Mesa: ")
                .append(mesa.getNumero())
                .append("\n");

        resultado.append("Pedido ID: ")
                .append(pedido.getId())
                .append("\n");

        resultado.append("Estado: ")
                .append(pedido.getEstado())
                .append("\n");

        resultado.append("Total: ")
                .append(pedido.getTotal())
                .append("\n");

        resultado.append("Fecha de creación: ")
                .append(pedido.getCreatedAt())
                .append("\n");

        resultado.append("\n===== PRODUCTOS =====\n");

        if (detalles.isEmpty()) {

            resultado.append("El pedido no tiene productos.\n");

        } else {

            for (DetallePedido detalle : detalles) {

                resultado.append("Producto: ")
                        .append(detalle.getPlato().getNombre())
                        .append("\n");

                resultado.append("Cantidad: ")
                        .append(detalle.getCantidad())
                        .append("\n");

                resultado.append("--------------------\n");
            }
        }

        return resultado.toString();
    }
    @Transactional(readOnly = true)
    public String consultarMesero(Long mesaId) {

        Mesa mesa = mesaRepository.findById(mesaId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Mesa no encontrada con ID: " + mesaId
                        )
                );

        if (mesa.getMesero() == null) {
            return "La mesa " + mesa.getNumero()
                    + " no tiene un mesero asignado.";
        }

        Mesero mesero = mesa.getMesero();

        return "Mesa: " + mesa.getNumero()
                + "\nMesero: " + mesero.getNombre()
                + "\nID del mesero: " + mesero.getId();
    }
}