package util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EstadoFormatterUtilTest {
    @Test
    void normalizaCodigosAntesDeMostrar() throws Exception {
        assertEquals("Pendiente de pago", EstadoFormatterUtil.formatearEstadoMesa(" pendiente-de-pago "));
        assertEquals("En preparación", EstadoFormatterUtil.formatearEstadoPedido("en preparacion"));
        assertEquals("Desconocido", EstadoFormatterUtil.formatearEstadoPedido(null));
    }
}

