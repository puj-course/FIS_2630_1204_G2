package service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MesaServiceTest {
    @Test
    void rechazaNumerosYCapacidadesInvalidos() throws Exception {
        var servicio = new MesaService();
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarMesa(0, 4));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarMesa(10000, 4));
        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarCapacidad(1, 0));
    }
}

