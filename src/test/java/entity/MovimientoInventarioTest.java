package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MovimientoInventarioTest {
    @Test
    void conservaTipoMovimiento() throws Exception {
        var registro = new MovimientoInventario();
        registro.setTipoMovimiento(enums.TipoMovimiento.REVERSO_COMANDA);
        assertEquals(enums.TipoMovimiento.REVERSO_COMANDA, registro.getTipoMovimiento());
    }
}

