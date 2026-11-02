package service.cocina;

import entity.Pedido;
import enums.EstadoPedido;
import exceptions.PedidoNoEditableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Servicio 3 de 3 — Tablero de cocina (patrón Adapter).
 *
 * Métodos bajo prueba:
 *   1. PedidoComandaAdapter.getColumna
 *   2. PedidoComandaAdapter.puedeAvanzar
 *   3. PedidoComandaAdapter.getTextoAccion
 *   4. PedidoComandaAdapter.avanzar
 *   5. PedidoComandaAdapter.getMinutosEspera
 *
 * Todas las pruebas siguen Arrange - Act - Assert y no tocan la base de datos.
 */
@DisplayName("Tablero de cocina (Adapter)")
class PedidoComandaAdapterTest {

    /** Construye un pedido de prueba en el estado indicado. */
    private Pedido pedido(EstadoPedido estado) {
        Pedido p = new Pedido();
        p.setId(1L);
        p.setNumeroPedido("P-001");
        p.setMesaId(7L);
        p.setEstado(estado);
        p.setFechaPedido(LocalDateTime.now().minusMinutes(12));
        return p;
    }

    // ---------------------------------------------------------------
    // Método 1: getColumna
    // ---------------------------------------------------------------

    @Test
    @DisplayName("CP22 (normal) — cada estado cae en su columna del tablero")
    void cadaEstadoEnSuColumna() {
        // Arrange
        ComandaCocina pendiente = new PedidoComandaAdapter(pedido(EstadoPedido.PENDIENTE));
        ComandaCocina asignada = new PedidoComandaAdapter(pedido(EstadoPedido.ASIGNADO_MESA));
        ComandaCocina enPreparacion = new PedidoComandaAdapter(pedido(EstadoPedido.EN_PREPARACION));
        ComandaCocina completada = new PedidoComandaAdapter(pedido(EstadoPedido.COMPLETADO));

        // Act
        ComandaCocina.Columna columnaPendiente = pendiente.getColumna();
        ComandaCocina.Columna columnaAsignada = asignada.getColumna();
        ComandaCocina.Columna columnaEnPreparacion = enPreparacion.getColumna();
        ComandaCocina.Columna columnaCompletada = completada.getColumna();

        // Assert
        assertEquals(ComandaCocina.Columna.PENDIENTES, columnaPendiente);
        assertEquals(ComandaCocina.Columna.PENDIENTES, columnaAsignada);
        assertEquals(ComandaCocina.Columna.PREPARACION, columnaEnPreparacion);
        assertEquals(ComandaCocina.Columna.LISTOS, columnaCompletada);
    }

    @Test
    @DisplayName("CP23 (negativa) — un pedido sin estado no tumba el tablero")
    void estadoNuloCaeEnPendientes() {
        // Arrange
        ComandaCocina comanda = new PedidoComandaAdapter(pedido(null));

        // Act
        ComandaCocina.Columna columna = comanda.getColumna();

        // Assert
        assertEquals(ComandaCocina.Columna.PENDIENTES, columna);
    }

    // ---------------------------------------------------------------
    // Método 2: puedeAvanzar
    // ---------------------------------------------------------------

    @Test
    @DisplayName("CP24 (normal) — los tres estados de cocina sí pueden avanzar")
    void losEstadosDeCocinaAvanzan() {
        // Arrange
        ComandaCocina pendiente = new PedidoComandaAdapter(pedido(EstadoPedido.PENDIENTE));
        ComandaCocina asignada = new PedidoComandaAdapter(pedido(EstadoPedido.ASIGNADO_MESA));
        ComandaCocina enPreparacion = new PedidoComandaAdapter(pedido(EstadoPedido.EN_PREPARACION));

        // Act
        boolean avanzaPendiente = pendiente.puedeAvanzar();
        boolean avanzaAsignada = asignada.puedeAvanzar();
        boolean avanzaEnPreparacion = enPreparacion.puedeAvanzar();

        // Assert
        assertTrue(avanzaPendiente);
        assertTrue(avanzaAsignada);
        assertTrue(avanzaEnPreparacion);
    }

    @Test
    @DisplayName("CP25 (borde) — un pedido COMPLETADO ya no tiene siguiente paso")
    void completadoNoAvanza() {
        // Arrange
        ComandaCocina comanda = new PedidoComandaAdapter(pedido(EstadoPedido.COMPLETADO));

        // Act
        boolean puede = comanda.puedeAvanzar();

        // Assert
        assertFalse(puede);
    }

    @Test
    @DisplayName("CP26 (borde) — un pedido ya cobrado o cancelado tampoco avanza")
    void pedidoFueraDeCocinaNoAvanza() {
        // Arrange
        ComandaCocina pagado = new PedidoComandaAdapter(pedido(EstadoPedido.PAGADO));
        ComandaCocina entregado = new PedidoComandaAdapter(pedido(EstadoPedido.ENTREGADO));
        ComandaCocina cancelado = new PedidoComandaAdapter(pedido(EstadoPedido.CANCELADO));

        // Act
        boolean avanzaPagado = pagado.puedeAvanzar();
        boolean avanzaEntregado = entregado.puedeAvanzar();
        boolean avanzaCancelado = cancelado.puedeAvanzar();

        // Assert
        assertFalse(avanzaPagado);
        assertFalse(avanzaEntregado);
        assertFalse(avanzaCancelado);
    }

    // ---------------------------------------------------------------
    // Método 3: getTextoAccion
    // ---------------------------------------------------------------

    @Test
    @DisplayName("CP27 (normal) — el botón dice el paso que sigue")
    void textoDelBotonSegunElEstado() {
        // Arrange
        ComandaCocina pendiente = new PedidoComandaAdapter(pedido(EstadoPedido.PENDIENTE));
        ComandaCocina enPreparacion = new PedidoComandaAdapter(pedido(EstadoPedido.EN_PREPARACION));
        ComandaCocina completada = new PedidoComandaAdapter(pedido(EstadoPedido.COMPLETADO));

        // Act
        String textoPendiente = pendiente.getTextoAccion();
        String textoEnPreparacion = enPreparacion.getTextoAccion();
        String textoCompletada = completada.getTextoAccion();

        // Assert
        assertEquals("Pasar a preparación", textoPendiente);
        assertEquals("Marcar como listo", textoEnPreparacion);
        assertEquals("—", textoCompletada);
    }

    // ---------------------------------------------------------------
    // Método 4: avanzar
    // ---------------------------------------------------------------

    @Test
    @DisplayName("CP28 (negocio) — pendiente pasa a preparación y preparación pasa a completado")
    void avanzarRecorreElFlujoDeCocina() {
        // Arrange
        Pedido p = pedido(EstadoPedido.PENDIENTE);
        ComandaCocina comanda = new PedidoComandaAdapter(p);

        // Act
        comanda.avanzar();
        EstadoPedido despuesDelPrimerPaso = p.getEstado();
        comanda.avanzar();

        // Assert
        assertEquals(EstadoPedido.EN_PREPARACION, despuesDelPrimerPaso);
        assertEquals(EstadoPedido.COMPLETADO, p.getEstado());
    }

    @Test
    @DisplayName("CP29 (excepción) — un pedido ENTREGADO no se puede devolver a la cocina")
    void entregadoNoVuelveAPreparacion() {
        // Arrange
        Pedido p = pedido(EstadoPedido.ENTREGADO);
        ComandaCocina comanda = new PedidoComandaAdapter(p);

        // Act + Assert
        assertThrows(PedidoNoEditableException.class, comanda::avanzar);
        assertEquals(EstadoPedido.ENTREGADO, p.getEstado());
    }

    @Test
    @DisplayName("CP30 (negativa) — construir una comanda sin pedido se rechaza")
    void comandaSinPedidoSeRechaza() {
        // Arrange
        Pedido ninguno = null;

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> new PedidoComandaAdapter(ninguno));
    }

    // ---------------------------------------------------------------
    // Método 5: getMinutosEspera
    // ---------------------------------------------------------------

    @Test
    @DisplayName("CP31 (normal) — un pedido de hace 12 minutos reporta 12 minutos")
    void minutosDeEspera() {
        // Arrange
        Pedido p = pedido(EstadoPedido.PENDIENTE);
        p.setFechaPedido(LocalDateTime.now().minusMinutes(12));
        ComandaCocina comanda = new PedidoComandaAdapter(p);

        // Act
        long minutos = comanda.getMinutosEspera();

        // Assert
        assertEquals(12L, minutos);
    }

    @Test
    @DisplayName("CP32 (borde) — un pedido sin fecha reporta 0 y no lanza excepción")
    void pedidoSinFechaReportaCero() {
        // Arrange
        Pedido p = pedido(EstadoPedido.PENDIENTE);
        p.setFechaPedido(null);
        ComandaCocina comanda = new PedidoComandaAdapter(p);

        // Act
        long minutos = comanda.getMinutosEspera();

        // Assert
        assertEquals(0L, minutos);
    }

    // ---------------------------------------------------------------
    // Validaciones del servicio que no tenían prueba detrás
    // ---------------------------------------------------------------

    @Test
    @DisplayName("CP40 (negativa) — un pedido sin estado no ofrece ninguna acción")
    void pedidoSinEstadoNoOfreceAccion() {
        // Arrange
        ComandaCocina comanda = new PedidoComandaAdapter(pedido(null));

        // Act
        String texto = comanda.getTextoAccion();

        // Assert
        assertEquals("—", texto);
    }

    @Test
    @DisplayName("CP41 (borde) — un pedido con fecha futura reporta 0 y no minutos negativos")
    void pedidoConFechaFuturaReportaCero() {
        // Arrange: pasa cuando el reloj de una caja esta adelantado respecto
        // al del servidor, o cuando la fecha se carga mal en la base.
        Pedido p = pedido(EstadoPedido.PENDIENTE);
        p.setFechaPedido(LocalDateTime.now().plusMinutes(30));
        ComandaCocina comanda = new PedidoComandaAdapter(p);

        // Act
        long minutos = comanda.getMinutosEspera();

        // Assert
        assertEquals(0L, minutos);
    }
}
