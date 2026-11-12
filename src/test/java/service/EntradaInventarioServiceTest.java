package service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntradaInventarioServiceTest {
    @Test
    void rechazaEntradasSinCantidadPositiva() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new EntradaInventarioService().registrar(new entity.EntradaInventario(1L, 0, "Compra")));
    }
}

