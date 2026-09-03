package com.restaurante.service;

import com.restaurante.entity.AlertaInventario;
import com.restaurante.entity.Ingrediente;
import com.restaurante.repository.AlertaInventarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AlertaInventarioService {

    private final AlertaInventarioRepository alertaRepository;

    public AlertaInventarioService(
            AlertaInventarioRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    @Transactional
    public void evaluarInventario(Ingrediente ingrediente) {

        BigDecimal stockActual = ingrediente.getStockActual();
        BigDecimal stockMinimo = ingrediente.getStockMinimo();

        // Verificar si el stock está por debajo o igual al mínimo
        boolean stockBajo =
                stockActual.compareTo(stockMinimo) <= 0;

        if (stockBajo) {

            // Comprobar si ya existe una alerta activa
            boolean alertaExiste =
                    alertaRepository
                            .findByIngredienteIdAndIsResolvedFalse(
                                    ingrediente.getId()
                            )
                            .isPresent();

            // Si no existe, crear una nueva alerta
            if (!alertaExiste) {

                AlertaInventario alerta =
                        new AlertaInventario();

                alerta.setIngrediente(ingrediente);

                alerta.setStockActual(stockActual);

                alerta.setStockMinimo(stockMinimo);

                BigDecimal cantidadSugerida =
                        stockMinimo.subtract(stockActual);

                // Evitar cantidades negativas
                if (cantidadSugerida.compareTo(BigDecimal.ZERO) < 0) {
                    cantidadSugerida = BigDecimal.ZERO;
                }

                alerta.setCantidadSugerida(cantidadSugerida);

                alerta.setIsResolved(false);

                alerta.setCreatedAt(LocalDateTime.now());

                alertaRepository.save(alerta);
            }
        }
    }
}