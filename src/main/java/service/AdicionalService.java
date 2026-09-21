package com.restaurante.service;

import com.restaurante.entity.Adicional;
import com.restaurante.entity.DetallePedido;
import com.restaurante.entity.DetallePedidoAdicional;
import com.restaurante.exception.AdicionalNoDisponibleException;
import com.restaurante.repository.AdicionalRepository;
import com.restaurante.repository.DetallePedidoAdicionalRepository;
import com.restaurante.repository.DetallePedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.math.BigDecimal;
import java.util.Set;

@Service
public class AdicionalService {

    private static final Set<EstadoPedido> ESTADOS_NO_EDITABLES = Set.of(
            EstadoPedido.COMPLETADO,
            EstadoPedido.ENTREGADO,
            EstadoPedido.CANCELADO
    );

    private final AdicionalRepository adicionalRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final DetallePedidoAdicionalRepository detallePedidoAdicionalRepository;

    public AdicionalService(
            AdicionalRepository adicionalRepository,
            DetallePedidoRepository detallePedidoRepository,
            DetallePedidoAdicionalRepository detallePedidoAdicionalRepository) {

        this.adicionalRepository = adicionalRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.detallePedidoAdicionalRepository = detallePedidoAdicionalRepository;
    }

    @Transactional
    public DetallePedidoAdicional agregarAdicional(
            Long detallePedidoId, Long adicionalId, Integer cantidad) {

        // Validar que la línea de detalle exista
        DetallePedido detallePedido =
            detallePedidoRepository.findById(detallePedidoId)
                .orElseThrow(
                    () -> new AdicionalNoDisponibleException("Línea de detalle no encontrada con id " + detallePedidoId)
                );

        // La comanda debe seguir activa para poder modificar sus líneas
        if (ESTADOS_NO_EDITABLES.contains(detallePedido.getPedido().getEstado())) {
            throw new PedidoNoEditableException(
                "No se pueden agregar adicionales: el pedido " +
                    detallePedido.getPedido().getNumeroPedido() +
                    " está en estado " + detallePedido.getPedido().getEstado()
            );
        }
        
        // Validar que el adicional exista en el catálogo
        Adicional adicional =
            adicionalRepository.findById(adicionalId)
                .orElseThrow(
                    () -> new AdicionalNoDisponibleException("Adicional no encontrado con id " + adicionalId)
                );

        // Rechazar adicionales que fueron desactivados del catálogo
        if (!adicional.getIsActive()) {
            throw new AdicionalNoDisponibleException(
                "El adicional " + adicional.getNombre() + " no está disponible"
            );
        }

        // Congelar el precio del adicional al momento de agregarlo
        DetallePedidoAdicional detallePedidoAdicional = new DetallePedidoAdicional();
        detallePedidoAdicional.setDetallePedido(detallePedido);
        detallePedidoAdicional.setAdicional(adicional);
        detallePedidoAdicional.setCantidad(cantidad != null ? cantidad : 1);
        detallePedidoAdicional.setPrecioAdicional(adicional.getPrecioAdicional());

        DetallePedidoAdicional guardado = detallePedidoAdicionalRepository.save(detallePedidoAdicional);
 
        // Reflejar el sobrecosto en el subtotal de la línea de pedido
        BigDecimal sobrecosto =
                adicional.getPrecioAdicional().multiply(BigDecimal.valueOf(cantidadFinal));
 
        detallePedido.setSubtotal(detallePedido.getSubtotal().add(sobrecosto));
        detallePedidoRepository.save(detallePedido);
 
        return guardado;
    }
}
