package service;

import entity.Adicional;
import entity.DetallePedidoAdicional;
import exceptions.AdicionalNoDisponibleException;
import repository.AdicionalRepository;
import repository.DetallePedidoAdicionalRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class AdicionalService {

    private final AdicionalRepository adicionalRepository = new AdicionalRepository();
    private final DetallePedidoAdicionalRepository detallePedidoAdicionalRepository =
            new DetallePedidoAdicionalRepository();

    public List<Adicional> listarActivos() throws SQLException {
        return adicionalRepository.findAllActivos();
    }

    public Adicional obtener(long id) throws SQLException {
        return adicionalRepository.findById(id)
                .orElseThrow(() -> new AdicionalNoDisponibleException("Adicional no encontrado: " + id));
    }


    public DetallePedidoAdicional agregarADetalle(long detallePedidoId, long adicionalId, int cantidad)
            throws SQLException {

        Adicional adicional = adicionalRepository.findById(adicionalId)
                .orElseThrow(() -> new AdicionalNoDisponibleException("Adicional no encontrado: " + adicionalId));

        if (Boolean.FALSE.equals(adicional.getIsActive())) {
            throw new AdicionalNoDisponibleException("Adicional inactivo: " + adicional.getNombre());
        }

        int cantidadFinal = cantidad > 0 ? cantidad : 1;

        DetallePedidoAdicional linea = new DetallePedidoAdicional();
        linea.setDetallePedidoId(detallePedidoId);
        linea.setAdicionalId(adicionalId);
        linea.setCantidad(cantidadFinal);

        return detallePedidoAdicionalRepository.save(linea);
    }

    public BigDecimal calcularSobrecosto(Adicional adicional, int cantidad) {
        if (adicional.getPrecioAdicional() == null) {
            return BigDecimal.ZERO;
        }
        return adicional.getPrecioAdicional().multiply(BigDecimal.valueOf(cantidad));
    }
}