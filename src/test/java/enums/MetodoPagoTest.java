package enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetodoPagoTest {
    @Test
    void conservaCodigosDeDominio() throws Exception {
        var codigos = Arrays.stream(MetodoPago.values()).map(Enum::name).collect(Collectors.toSet());
        assertEquals(Set.of("EFECTIVO", "TARJETA", "TRANSFERENCIA"), codigos);
    }
}

