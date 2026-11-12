package service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListaEsperaServiceTest {
    @Test
    void rechazaGruposVacios() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new ListaEsperaService().registrarEnEspera("Ana", 0));
    }
}

