package entity;

import enums.TipoMovimiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MovimientoInventario es cada descuento o devolucion de inventario que genera
 * una comanda. Es la contraparte formal de EntradaInventario.
 */
class MovimientoInventarioTest {

    private MovimientoInventario movimiento(TipoMovimiento tipo, String cantidad) {
        var m = new MovimientoInventario();
        m.setIngredienteId(10L);
        m.setTipoMovimiento(tipo);
        m.setCantidad(new BigDecimal(cantidad));
        m.setUsuarioId(9L);
        return m;
    }

    @Test
    void conservaElTipoYLaCantidad() throws Exception {
        var m = movimiento(TipoMovimiento.VENTA_COMANDA, "2.5");
        assertEquals(TipoMovimiento.VENTA_COMANDA, m.getTipoMovimiento());
        assertEquals(new BigDecimal("2.5"), m.getCantidad());
    }

    @Test
    @DisplayName("un movimiento nuevo se marca solo con la fecha")
    void seMarcaSoloConLaFecha() {
        var antes = LocalDateTime.now().minusSeconds(5);
        assertTrue(new MovimientoInventario().getFechaMovimiento().isAfter(antes));
    }

    @Test
    @DisplayName("una venta descuenta y un reverso devuelve")
    void laVentaDescuentaYElReversoDevuelve() {
        var venta = movimiento(TipoMovimiento.VENTA_COMANDA, "2.5");
        var reverso = movimiento(TipoMovimiento.REVERSO_COMANDA, "2.5");
        assertNotEquals(venta.getTipoMovimiento(), reverso.getTipoMovimiento());
        assertEquals(venta.getCantidad(), reverso.getCantidad());
    }

    @Test
    @DisplayName("el movimiento es de un ingrediente o de un producto, no de los dos")
    void esDeUnoODelOtro() {
        var deIngrediente = movimiento(TipoMovimiento.VENTA_COMANDA, "1");
        assertNotNull(deIngrediente.getIngredienteId());
        assertNull(deIngrediente.getProductoId());

        var deProducto = new MovimientoInventario();
        deProducto.setProductoId(20L);
        assertNotNull(deProducto.getProductoId());
        assertNull(deProducto.getIngredienteId());
    }

    @Test
    @DisplayName("guarda quien hizo el movimiento")
    void guardaQuienLoHizo() {
        assertEquals(9L, movimiento(TipoMovimiento.VENTA_COMANDA, "1").getUsuarioId());
    }

    @Test
    @DisplayName("usa BigDecimal, no double, porque las cantidades no se pueden redondear")
    void usaBigDecimal() {
        var m = movimiento(TipoMovimiento.VENTA_COMANDA, "0.10");
        assertEquals(2, m.getCantidad().scale());
        assertEquals(new BigDecimal("0.10"), m.getCantidad());
    }

    @Test
    @DisplayName("se puede fijar la fecha a mano al leerlo de la base")
    void seFijaLaFechaAlLeerlo() {
        var cuando = LocalDateTime.of(2026, 11, 10, 20, 30);
        var m = movimiento(TipoMovimiento.VENTA_COMANDA, "1");
        m.setFechaMovimiento(cuando);
        assertEquals(cuando, m.getFechaMovimiento());
    }

    @Test
    @DisplayName("guarda el motivo cuando hay uno que explicar")
    void guardaElMotivo() {
        var m = movimiento(TipoMovimiento.REVERSO_COMANDA, "1");
        m.setMotivo("Se cancelo el pedido PED-0007");
        assertTrue(m.getMotivo().contains("PED-0007"));
    }

    @Test
    @DisplayName("un movimiento nuevo nace sin tipo ni cantidad")
    void naceSinTipoNiCantidad() {
        var m = new MovimientoInventario();
        assertNull(m.getId());
        assertNull(m.getTipoMovimiento());
        assertNull(m.getCantidad());
    }
}
