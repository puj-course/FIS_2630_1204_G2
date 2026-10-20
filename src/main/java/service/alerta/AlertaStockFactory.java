package service.alerta;

import entity.Ingrediente;

import java.math.BigDecimal;

/**
 * Patrón Factory: decide qué tipo de alerta crear según el stock del
 * ingrediente, y así la pantalla no necesita comparar cantidades ni conocer
 * las clases concretas.
 *
 * Reglas:
 *   - sin stock (cero o menos)            -> AlertaAgotado
 *   - stock menor o igual al mínimo       -> AlertaStockBajo
 *   - cualquier otro caso                 -> AlertaNormal
 */
public class AlertaStockFactory {

    private AlertaStockFactory() {
    }

    public static AlertaStock crear(Ingrediente ingrediente) {
        if (ingrediente == null || ingrediente.getStockActual() == null) {
            return new AlertaNormal();
        }

        if (ingrediente.getStockActual().compareTo(BigDecimal.ZERO) <= 0) {
            return new AlertaAgotado();
        }

        if (ingrediente.tieneStockBajo()) {
            return new AlertaStockBajo();
        }

        return new AlertaNormal();
    }
}
