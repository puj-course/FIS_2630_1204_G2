package enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadoMesaTest {
    @Test
    void conservaCodigosDeDominio() throws Exception {
        var codigos = Arrays.stream(EstadoMesa.values()).map(Enum::name).collect(Collectors.toSet());
        assertEquals(Set.of("DISPONIBLE", "OCUPADA", "EN_LIMPIEZA", "RESERVADA"), codigos);
    }
}

