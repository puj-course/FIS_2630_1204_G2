package enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TipoMovimientoTest {
    @Test
    void conservaCodigosDeDominio() throws Exception {
        var codigos = Arrays.stream(TipoMovimiento.values()).map(Enum::name).collect(Collectors.toSet());
        assertEquals(Set.of("VENTA_COMANDA", "REVERSO_COMANDA"), codigos);
    }
}

