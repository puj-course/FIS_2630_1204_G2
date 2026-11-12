package service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AsignacionMesaServiceTest {
    @Test
    void rechazaUnaSegundaAsignacionActiva() throws Exception {
        try (var construccion = mockConstruction(repository.AsignacionMesaRepository.class,
                (repo, contexto) -> when(repo.findActivaByMesaId(3L)).thenReturn(java.util.Optional.of(new entity.AsignacionMesa())))) {
            var servicio = new AsignacionMesaService();
            assertThrows(exceptions.AsignacionActivaException.class, () -> servicio.asignar(3L, 9L));
            verify(construccion.constructed().getFirst(), never()).save(any());
        }
    }
}

