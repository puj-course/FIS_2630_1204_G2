package enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EstadoListaEspera dice en que punto va un grupo que esta esperando mesa.
 *
 * Los nombres de las constantes viajan a la base de datos tal cual, asi que
 * cambiarlos rompe los datos que ya estan guardados. Por eso se fijan aqui.
 */
class EstadoListaEsperaTest {

    @Test
    void conservaCodigosDeDominio() throws Exception {
        var codigos = Arrays.stream(EstadoListaEspera.values()).map(Enum::name).collect(Collectors.toSet());
        assertEquals(Set.of("EN_ESPERA", "ASIGNADO", "CANCELADO"), codigos);
    }

    @Test
    @DisplayName("tiene exactamente 3 estados, ni uno mas")
    void tieneLaCantidadEsperada() {
        assertEquals(3, EstadoListaEspera.values().length);
    }

    @Test
    @DisplayName("se reconstruye desde el texto que viene de la base")
    void seReconstruyeDesdeElTexto() {
        assertSame(EstadoListaEspera.EN_ESPERA, EstadoListaEspera.valueOf("EN_ESPERA"));
    }

    @Test
    @DisplayName("name() y valueOf() dan la vuelta completa sin perder nada")
    void laVueltaCompletaNoPierdeNada() {
        for (EstadoListaEspera valor : EstadoListaEspera.values()) {
            assertSame(valor, EstadoListaEspera.valueOf(valor.name()));
        }
    }

    @Test
    @DisplayName("un texto que no corresponde a ningun estado revienta de una")
    void textoDesconocidoRevienta() {
        // Es a proposito: preferimos el error aqui que un estado inventado
        // circulando por la aplicacion.
        assertThrows(IllegalArgumentException.class, () -> EstadoListaEspera.valueOf("CUALQUIER_COSA"));
        assertThrows(IllegalArgumentException.class, () -> EstadoListaEspera.valueOf("en_espera"));
    }

    @Test
    @DisplayName("ningun estado se repite y todos tienen una posicion distinta")
    void ningunoSeRepite() {
        var valores = EstadoListaEspera.values();
        var nombres = Arrays.stream(valores).map(Enum::name).collect(Collectors.toSet());
        var posiciones = Arrays.stream(valores).map(Enum::ordinal).collect(Collectors.toSet());
        assertEquals(valores.length, nombres.size());
        assertEquals(valores.length, posiciones.size());
    }

    @Test
    @DisplayName("se comparan con == porque solo hay una instancia de cada uno")
    void seComparanConIgualIgual() {
        assertSame(EstadoListaEspera.valueOf("EN_ESPERA"), EstadoListaEspera.valueOf("EN_ESPERA"));
    }
}
