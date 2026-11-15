package enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EstadoReserva dice en que va una reserva desde que se toma hasta que el grupo llega.
 *
 * Los nombres de las constantes viajan a la base de datos tal cual, asi que
 * cambiarlos rompe los datos que ya estan guardados. Por eso se fijan aqui.
 */
class EstadoReservaTest {

    @Test
    void conservaCodigosDeDominio() throws Exception {
        var codigos = Arrays.stream(EstadoReserva.values()).map(Enum::name).collect(Collectors.toSet());
        assertEquals(Set.of("ACTIVA", "CANCELADA", "COMPLETADA"), codigos);
    }

    @Test
    @DisplayName("tiene exactamente 3 estados, ni uno mas")
    void tieneLaCantidadEsperada() {
        assertEquals(3, EstadoReserva.values().length);
    }

    @Test
    @DisplayName("se reconstruye desde el texto que viene de la base")
    void seReconstruyeDesdeElTexto() {
        assertSame(EstadoReserva.ACTIVA, EstadoReserva.valueOf("ACTIVA"));
    }

    @Test
    @DisplayName("name() y valueOf() dan la vuelta completa sin perder nada")
    void laVueltaCompletaNoPierdeNada() {
        for (EstadoReserva valor : EstadoReserva.values()) {
            assertSame(valor, EstadoReserva.valueOf(valor.name()));
        }
    }

    @Test
    @DisplayName("un texto que no corresponde a ningun estado revienta de una")
    void textoDesconocidoRevienta() {
        // Es a proposito: preferimos el error aqui que un estado inventado
        // circulando por la aplicacion.
        assertThrows(IllegalArgumentException.class, () -> EstadoReserva.valueOf("CUALQUIER_COSA"));
        assertThrows(IllegalArgumentException.class, () -> EstadoReserva.valueOf("activa"));
    }

    @Test
    @DisplayName("ningun estado se repite y todos tienen una posicion distinta")
    void ningunoSeRepite() {
        var valores = EstadoReserva.values();
        var nombres = Arrays.stream(valores).map(Enum::name).collect(Collectors.toSet());
        var posiciones = Arrays.stream(valores).map(Enum::ordinal).collect(Collectors.toSet());
        assertEquals(valores.length, nombres.size());
        assertEquals(valores.length, posiciones.size());
    }

    @Test
    @DisplayName("se comparan con == porque solo hay una instancia de cada uno")
    void seComparanConIgualIgual() {
        assertSame(EstadoReserva.valueOf("ACTIVA"), EstadoReserva.valueOf("ACTIVA"));
    }
}
