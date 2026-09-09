package com.restaurante.service;

import com.restaurante.enums.EstadoMesa;
import com.restaurante.entity.Mesa;
import com.restaurante.entity.Pedido;
import com.restaurante.enums.EstadoPedido;
import com.restaurante.repository.MesaRepository;
import com.restaurante.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MesaService {

    private final MesaRepository mesaRepository;
    private final PedidoRepository pedidoRepository;

    public MesaService(
            MesaRepository mesaRepository,
            PedidoRepository pedidoRepository
    ) {
        this.mesaRepository = mesaRepository;
        this.pedidoRepository = pedidoRepository;
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
}