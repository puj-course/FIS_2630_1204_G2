package enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TipoMovimiento dice si el movimiento de inventario descuenta por una venta o devuelve por una anulacion.
 *
 * Los nombres de las constantes viajan a la base de datos tal cual, asi que
 * cambiarlos rompe los datos que ya estan guardados. Por eso se fijan aqui.
 */
class TipoMovimientoTest {

    @Test
    void conservaCodigosDeDominio() throws Exception {
        var codigos = Arrays.stream(TipoMovimiento.values()).map(Enum::name).collect(Collectors.toSet());
        assertEquals(Set.of("VENTA_COMANDA", "REVERSO_COMANDA"), codigos);
    }

    @Test
    @DisplayName("tiene exactamente 2 estados, ni uno mas")
    void tieneLaCantidadEsperada() {
        assertEquals(2, TipoMovimiento.values().length);
    }

    @Test
    @DisplayName("se reconstruye desde el texto que viene de la base")
    void seReconstruyeDesdeElTexto() {
        assertSame(TipoMovimiento.VENTA_COMANDA, TipoMovimiento.valueOf("VENTA_COMANDA"));
    }

    @Test
    @DisplayName("name() y valueOf() dan la vuelta completa sin perder nada")
    void laVueltaCompletaNoPierdeNada() {
        for (TipoMovimiento valor : TipoMovimiento.values()) {
            assertSame(valor, TipoMovimiento.valueOf(valor.name()));
        }
    }

    @Test
    @DisplayName("un texto que no corresponde a ningun estado revienta de una")
    void textoDesconocidoRevienta() {
        // Es a proposito: preferimos el error aqui que un estado inventado
        // circulando por la aplicacion.
        assertThrows(IllegalArgumentException.class, () -> TipoMovimiento.valueOf("CUALQUIER_COSA"));
        assertThrows(IllegalArgumentException.class, () -> TipoMovimiento.valueOf("venta_comanda"));
    }

    @Test
    @DisplayName("ningun estado se repite y todos tienen una posicion distinta")
    void ningunoSeRepite() {
        var valores = TipoMovimiento.values();
        var nombres = Arrays.stream(valores).map(Enum::name).collect(Collectors.toSet());
        var posiciones = Arrays.stream(valores).map(Enum::ordinal).collect(Collectors.toSet());
        assertEquals(valores.length, nombres.size());
        assertEquals(valores.length, posiciones.size());
    }

    @Test
    @DisplayName("se comparan con == porque solo hay una instancia de cada uno")
    void seComparanConIgualIgual() {
        assertSame(TipoMovimiento.valueOf("VENTA_COMANDA"), TipoMovimiento.valueOf("VENTA_COMANDA"));
    }
}
