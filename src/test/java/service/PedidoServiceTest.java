package service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PedidoServiceTest {
    @Test
    void rechazaCrearUnPedidoSiLaMesaTieneComanda() throws Exception {
        try (var construccion = mockConstruction(repository.PedidoRepository.class,
                (repo, contexto) -> when(repo.existsByMesaIdAndEstadoNot(eq(3L), any())).thenReturn(true))) {
            var servicio = new PedidoService();
            assertThrows(exceptions.MesaOcupadaException.class, () -> servicio.crearPedido(3L, 9L));
            verify(construccion.constructed().getFirst(), never()).save(any());
        }
    }
}

