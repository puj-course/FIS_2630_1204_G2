package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AlertaInventario es el aviso que se le crea al administrador cuando un
 * ingrediente baja del minimo. Vive en dos estados: PENDIENTE y RESUELTA.
 */
class AlertaInventarioTest {

    private AlertaInventario alertaEn(String estado) {
        var a = new AlertaInventario();
        a.setEstado(estado);
        return a;
    }

    @Test
    void conservaElEstadoDeLaAlerta() throws Exception {
        assertEquals("PENDIENTE", alertaEn("PENDIENTE").getEstado());
    }

    // ------------------------------------------------------------ estado --

    @Test
    @DisplayName("una alerta en PENDIENTE esta pendiente y no resuelta")
    void laPendienteEstaPendiente() {
        var a = alertaEn("PENDIENTE");
        assertTrue(a.estaPendiente());
        assertFalse(a.estaResuelta());
    }

    @Test
    @DisplayName("una alerta en RESUELTA esta resuelta y no pendiente")
    void laResueltaEstaResuelta() {
        var a = alertaEn("RESUELTA");
        assertTrue(a.estaResuelta());
        assertFalse(a.estaPendiente());
    }

    @Test
    @DisplayName("no le importan las mayusculas del estado")
    void noLeImportanLasMayusculas() {
        assertTrue(alertaEn("pendiente").estaPendiente());
        assertTrue(alertaEn("Pendiente").estaPendiente());
        assertTrue(alertaEn("resuelta").estaResuelta());
    }

    @Test
    @DisplayName("un estado desconocido no es ni lo uno ni lo otro")
    void unEstadoDesconocidoNoEsNinguno() {
        var a = alertaEn("EN_REVISION");
        assertFalse(a.estaPendiente());
        assertFalse(a.estaResuelta());
    }

    @Test
    @DisplayName("sin estado tampoco revienta")
    void sinEstadoNoRevienta() {
        // La comparacion se hace con el texto fijo a la izquierda justamente
        // para que el nulo no tumbe la pantalla.
        var a = alertaEn(null);
        assertFalse(a.estaPendiente());
        assertFalse(a.estaResuelta());
    }

    @Test
    @DisplayName("una alerta no puede estar pendiente y resuelta al mismo tiempo")
    void nuncaEstaEnLosDosEstados() {
        for (String estado : new String[]{"PENDIENTE", "RESUELTA", "OTRO", null}) {
            var a = alertaEn(estado);
            assertFalse(a.estaPendiente() && a.estaResuelta(), "estado " + estado);
        }
    }

    // ------------------------------------------------------------- datos --

    @Test
    @DisplayName("guarda por que se disparo: el stock contra el minimo")
    void guardaElMotivoDelAviso() {
        var a = new AlertaInventario();
        a.setIngredienteId(10L);
        a.setStockActual(new BigDecimal("2.0"));
        a.setStockMinimo(new BigDecimal("5.0"));
        assertEquals(10L, a.getIngredienteId());
        assertTrue(a.getStockActual().compareTo(a.getStockMinimo()) < 0);
    }

    @Test
    @DisplayName("sugiere cuanto comprar para volver al minimo")
    void sugiereCuantoComprar() {
        var a = new AlertaInventario();
        a.setCantidadSugerida(new BigDecimal("3.0"));
        assertEquals(new BigDecimal("3.0"), a.getCantidadSugerida());
    }

    @Test
    @DisplayName("una alerta pendiente no tiene fecha de resolucion")
    void laPendienteNoTieneFechaDeCierre() {
        var a = alertaEn("PENDIENTE");
        a.setCreatedAt(LocalDateTime.now());
        assertNotNull(a.getCreatedAt());
        assertNull(a.getResolvedAt());
    }

    @Test
    @DisplayName("al resolverla se le pone la fecha de cierre")
    void alResolverlaSeLePoneLaFecha() {
        var a = alertaEn("PENDIENTE");
        var creada = LocalDateTime.now().minusHours(2);
        a.setCreatedAt(creada);

        a.setEstado("RESUELTA");
        a.setResolvedAt(LocalDateTime.now());

        assertTrue(a.estaResuelta());
        assertNotNull(a.getResolvedAt());
        assertTrue(a.getResolvedAt().isAfter(a.getCreatedAt()));
    }

    @Test
    @DisplayName("la alerta puede ser de un ingrediente o de un producto")
    void puedeSerDeIngredienteODeProducto() {
        var deIngrediente = new AlertaInventario();
        deIngrediente.setIngredienteId(10L);
        assertNotNull(deIngrediente.getIngredienteId());
        assertNull(deIngrediente.getProductoId());

        var deProducto = new AlertaInventario();
        deProducto.setProductoId(20L);
        assertNotNull(deProducto.getProductoId());
        assertNull(deProducto.getIngredienteId());
    }
}
