package service.descuento;

import java.math.BigDecimal;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DescuentoStrategyTest {
    @Test
    void permiteDelegarElCalculoEnLaEstrategiaElegida() throws Exception {
        var estrategia = mock(DescuentoStrategy.class);
        when(estrategia.calcular(BigDecimal.TEN, BigDecimal.ONE)).thenReturn(new BigDecimal("1.25"));
        assertEquals(new BigDecimal("1.25"), new CalculadoraDescuento(estrategia).calcular(BigDecimal.TEN, BigDecimal.ONE));
        verify(estrategia).calcular(BigDecimal.TEN, BigDecimal.ONE);
    }
}

