package service;

import java.sql.SQLException;
import models.ReglasDescuento.ReglaDescuento;
import repositories.ReglasDescuento.ReglaDescuentoRepository;

public class ReglaDescuentoService {// Instancia del repositorio para interactuar con la base de datos
    private ReglaDescuentoRepository repo = new ReglaDescuentoRepository();

    public void registrar(ReglaDescuento regla) throws SQLException {// Validaciones de los campos de la regla
        if (regla.getValor() < 0) {
            throw new IllegalArgumentException("El valor no puede ser negativo");
        }
        if (regla.getTipoCambio() == null || regla.getTipoCambio().isBlank()) {
            throw new IllegalArgumentException("Debe indicar el tipo de cambio");
        }
        repo.guardar(regla);
    }

    public double calcularPrecioFinal(double precioBase, ReglaDescuento regla) {// Calcula el precio final aplicando la regla de descuento
        if (regla.isEsPorcentaje()) {
            return precioBase - (precioBase * regla.getValor() / 100);
        } else {
            return precioBase - regla.getValor();
        }
    }
}
