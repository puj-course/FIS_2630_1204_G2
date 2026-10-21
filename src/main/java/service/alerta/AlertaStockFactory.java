package service.alerta;

import entity.Ingrediente;

import java.math.BigDecimal;

/**
 * Patrón Factory Method (GoF) - rol Creator.
 *
 * Define la interfaz para crear una AlertaStock (el factory method
 * {@link #crearAlerta()}) pero delega en las subclases la decisión de qué
 * clase concreta instanciar:
 *
 *   AlertaAgotadoFactory   -> AlertaAgotado
 *   AlertaStockBajoFactory -> AlertaStockBajo
 *   AlertaNormalFactory    -> AlertaNormal
 *
 * {@link #para(Ingrediente)} solo elige cuál de las fábricas concretas
 * corresponde al stock del ingrediente; no crea ninguna alerta por sí misma.
 */
public abstract class AlertaStockFactory {

    /** Factory Method: cada subclase decide qué alerta concreta crear. */
    public abstract AlertaStock crearAlerta();

    /** Elige la fábrica concreta según el stock del ingrediente. */
    public static AlertaStockFactory para(Ingrediente ingrediente) {
        if (ingrediente == null || ingrediente.getStockActual() == null) {
            return new AlertaNormalFactory();
        }

        if (ingrediente.getStockActual().compareTo(BigDecimal.ZERO) <= 0) {
            return new AlertaAgotadoFactory();
        }

        if (ingrediente.tieneStockBajo()) {
            return new AlertaStockBajoFactory();
        }

        return new AlertaNormalFactory();
    }
}
