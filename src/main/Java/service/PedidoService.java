package com.restaurante.service;

import com.restaurante.entity.EstadoMesa;
import com.restaurante.entity.EstadoPedido;
import com.restaurante.entity.Mesa;
import com.restaurante.entity.Pedido;
import com.restaurante.exception.MesaNotFoundException;
import com.restaurante.exception.MesaOcupadaException;
import com.restaurante.repository.EstadoMesaRepository;
import com.restaurante.repository.MesaRepository;
import com.restaurante.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class PedidoService {

    private static final Set<EstadoPedido> ESTADOS_YA_CERRADOS = Set.of(
            EstadoPedido.COMPLETADO,
            EstadoPedido.ENTREGADO,
            EstadoPedido.CANCELADO
    );

    private final PedidoRepository pedidoRepository;
    private final MesaRepository mesaRepository;
    private final MesaService mesaService;

    public PedidoService(
            PedidoRepository pedidoRepository,
            MesaRepository mesaRepository,
            MesaService mesaService) {

        this.pedidoRepository = pedidoRepository;
        this.mesaRepository = mesaRepository;
        this.mesaService = mesaService;
    }

    @Transactional
    public Pedido crearPedido(Long mesaId, Long usuarioId) {

        Mesa mesa =
            mesaRepository.findById(mesaId)
                .orElseThrow(
                    () -> new MesaNotFoundException("Mesa no encontrada con id " + mesaId)
                );

         // Verificar que la mesa esté libre antes de asignarle una comanda
        if (!"LIBRE".equals(mesa.getEstado().getCodigoEstado())) {
            throw new MesaOcupadaException(
                "La mesa " + mesa.getNumeroMesa() + " no está disponible"
            );
        }

        // Comprobar que no exista ya una comanda activa en esa mesa
        boolean tieneComandaActiva =
            pedidoRepository.existsByMesaIdAndEstadoNot(mesaId, EstadoPedido.CANCELADO);

        if (tieneComandaActiva) {
            throw new MesaOcupadaException(
                "La mesa " + mesa.getNumeroMesa() + " ya tiene una comanda activa"
            );
        }

        // Crear la comanda asociada a la mesa
        Pedido pedido = new Pedido();
        pedido.setMesa(mesa);
        pedido.setUsuarioId(usuarioId);
        pedido.setNumeroPedido("PED-" + System.currentTimeMillis());

        Pedido pedidoGuardado = pedidoRepository.save(pedido);

        // Apertura de comanda: la mesa pasa a OCUPADA, con auditoría del cambio
        mesaService.cambiarEstado(mesaId, "OCUPADA", "APERTURA_PEDIDO");

        return pedidoGuardado;
    }

    @Transactional
    public Pedido cerrarPedido(Long pedidoId, String codigoEstadoDestinoMesa) {

        Pedido pedido =
                pedidoRepository.findById(pedidoId)
                        .orElseThrow(
                                () -> new MesaNotFoundException("Pedido no encontrado con id " + pedidoId)
                        );

        if (ESTADOS_YA_CERRADOS.contains(pedido.getEstado())) {
            throw new PedidoNoEditableException(
                    "El pedido " + pedido.getNumeroPedido() +
                            " ya está en estado " + pedido.getEstado()
            );
        }

        pedido.setEstado(EstadoPedido.COMPLETADO);
        Pedido pedidoCerrado = pedidoRepository.save(pedido);

        // Cierre de comanda: la mesa pasa a DISPONIBLE (o EN_LIMPIEZA si se indica),
        String destino = codigoEstadoDestinoMesa != null ? codigoEstadoDestinoMesa : "LIBRE";
        mesaService.cambiarEstado(pedido.getMesa().getId(), destino, "CIERRE_PEDIDO");

        return pedidoCerrado;
    }
}