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

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final MesaRepository mesaRepository;
    private final EstadoMesaRepository estadoMesaRepository;

    public PedidoService(
            PedidoRepository pedidoRepository,
            MesaRepository mesaRepository,
            EstadoMesaRepository estadoMesaRepository) {

        this.pedidoRepository = pedidoRepository;
        this.mesaRepository = mesaRepository;
        this.estadoMesaRepository = estadoMesaRepository;
    }

    @Transactional
    public Pedido crearPedido(Long mesaId, Long usuarioId) {

        Mesa mesa =
            mesaRepository.findById(mesaId)
                .orElseThrow(
                    () -> new MesaNotFoundException("Mesa no encontrada con id " + mesaId)
                );

         // Verificar que la mesa esté libre antes de asignarle una comanda
        if (!"DISPONIBLE".equals(mesa.getEstado().getCodigoEstado())) {
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

        // Al crear la comanda, la mesa pasa a estar ocupada
        EstadoMesa ocupada =
            estadoMesaRepository.findByCodigoEstado("OCUPADA")
                .orElseThrow(
                    () -> new MesaNotFoundException("El código OCUPADA no existe en estados_mesa")
                );

        mesa.setEstado(ocupada);
        mesaRepository.save(mesa);

        return pedidoGuardado;
    }
}
