package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DetallePedidoAdicional une un renglon del pedido con un adicional: dice que
 * ese plato lleva queso extra, y cuantas porciones.
 */
class DetallePedidoAdicionalTest {

    private DetallePedidoAdicional union(Long renglon, Long adicional, Integer cantidad) {
        var d = new DetallePedidoAdicional();
        d.setDetallePedidoId(renglon);
        d.setAdicionalId(adicional);
        d.setCantidad(cantidad);
        return d;
    }

    @Test
    void conservaLasDosPuntas() throws Exception {
        var d = union(10L, 3L, 1);
        assertEquals(10L, d.getDetallePedidoId());
        assertEquals(3L, d.getAdicionalId());
    }

    @Test
    @DisplayName("guarda cuantas porciones del adicional van")
    void guardaCuantasPorciones() {
        assertEquals(2, union(10L, 3L, 2).getCantidad());
    }

    @Test
    @DisplayName("una union nueva nace vacia")
    void naceVacia() {
        var d = new DetallePedidoAdicional();
        assertNull(d.getId());
        assertNull(d.getDetallePedidoId());
        assertNull(d.getAdicionalId());
        assertNull(d.getCantidad());
    }

    @Test
    @DisplayName("la cantidad admite nulo porque es Integer, no int")
    void laCantidadAdmiteNulo() {
        // Queda documentado: quien la lea tiene que revisar el nulo antes de
        // multiplicar.
        assertNull(union(10L, 3L, null).getCantidad());
    }

    @Test
    @DisplayName("al guardarla la base le asigna el identificador")
    void alGuardarlaRecibeIdentificador() {
        var d = union(10L, 3L, 1);
        d.setId(55L);
        assertEquals(55L, d.getId());
    }

    @Test
    @DisplayName("un mismo renglon puede llevar dos adicionales distintos")
    void unRenglonPuedeLlevarVarios() {
        var queso = union(10L, 3L, 1);
        var arroz = union(10L, 4L, 1);
        assertEquals(queso.getDetallePedidoId(), arroz.getDetallePedidoId());
        assertNotEquals(queso.getAdicionalId(), arroz.getAdicionalId());
    }
}
