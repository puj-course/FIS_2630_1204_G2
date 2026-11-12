package enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadoListaEsperaTest {
    @Test
    void conservaCodigosDeDominio() throws Exception {
        var codigos = Arrays.stream(EstadoListaEspera.values()).map(Enum::name).collect(Collectors.toSet());
        assertEquals(Set.of("EN_ESPERA", "ASIGNADO", "CANCELADO"), codigos);
    }
}

