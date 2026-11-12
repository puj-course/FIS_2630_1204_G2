package entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ListaEsperaTest {
    @Test
    void conservaEstado() throws Exception {
        var registro = new ListaEspera();
        registro.setEstado(enums.EstadoListaEspera.ASIGNADO);
        assertEquals(enums.EstadoListaEspera.ASIGNADO, registro.getEstado());
    }
}

