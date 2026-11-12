package enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadoPedidoTest {
    @Test
    void conservaCodigosDeDominio() throws Exception {
        var codigos = Arrays.stream(EstadoPedido.values()).map(Enum::name).collect(Collectors.toSet());
        assertEquals(Set.of("PENDIENTE", "ASIGNADO_MESA", "EN_PREPARACION", "PAGADO", "COMPLETADO", "ENTREGADO", "CANCELADO"), codigos);
    }
}

