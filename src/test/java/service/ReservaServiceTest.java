package service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReservaServiceTest {
    @Test
    void rechazaCancelarUnaReservaInexistente() throws Exception {
        try (var construccion = mockConstruction(repository.ReservaRepository.class,
                (repo, contexto) -> when(repo.findById(7)).thenReturn(java.util.Optional.empty()))) {
            var servicio = new ReservaService();
            assertThrows(exceptions.ReservaNotFoundException.class, () -> servicio.cancelarReserva(7));
            verify(construccion.constructed().getFirst()).findById(7);
        }
    }
}

