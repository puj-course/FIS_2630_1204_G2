package com.restaurante.service;

import com.restaurante.entity.Adicional;
import com.restaurante.entity.DetallePedido;
import com.restaurante.entity.DetallePedidoAdicional;
import com.restaurante.exception.AdicionalNoDisponibleException;
import com.restaurante.repository.AdicionalRepository;
import com.restaurante.repository.DetallePedidoAdicionalRepository;
import com.restaurante.repository.DetallePedidoRepository;
import org.springframework.stereotype.Service;

@Service
public class AdicionalService {

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

    public DetallePedidoAdicional agregarAdicional(
            Long detallePedidoId, Long adicionalId, Integer cantidad) {

        DetallePedido detallePedido =
                detallePedidoRepository.findById(detallePedidoId)
                        .orElseThrow(
                                () -> new AdicionalNoDisponibleException(
                                        "Línea de detalle no encontrada con id " + detallePedidoId
                                )
                        );

        Adicional adicional =
                adicionalRepository.findById(adicionalId)
                        .orElseThrow(
                                () -> new AdicionalNoDisponibleException(
                                        "Adicional no encontrado con id " + adicionalId
                                )
                        );

        if (!adicional.getIsActive()) {
            throw new AdicionalNoDisponibleException(
                    "El adicional " + adicional.getNombre() + " no está disponible"
            );
        }

        DetallePedidoAdicional detallePedidoAdicional = new DetallePedidoAdicional();
        detallePedidoAdicional.setDetallePedido(detallePedido);
        detallePedidoAdicional.setAdicional(adicional);
        detallePedidoAdicional.setCantidad(cantidad != null ? cantidad : 1);
        detallePedidoAdicional.setPrecioAdicional(adicional.getPrecioAdicional());

        return detallePedidoAdicionalRepository.save(detallePedidoAdicional);
    }
}
