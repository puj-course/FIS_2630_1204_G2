package dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProductoDisponibilidad es lo que responde la consulta de disponibilidad:
 * dice si el producto se puede vender y, cuando no, por que no.
 */
class ProductoDisponibilidadTest {

    private ProductoDisponibilidad disponibilidadCon(String estado, String motivo) {
        return new ProductoDisponibilidad(4L, "P-4", "Sopa", "Entrada", estado, motivo);
    }

    @Test
    void conservaElMotivoDeIndisponibilidad() throws Exception {
        var dto = disponibilidadCon("AGOTADO", "Sin insumos");
        assertEquals(4L, dto.getProductoId());
        assertEquals("AGOTADO", dto.getEstado());
        assertEquals("Sin insumos", dto.getMotivo());
    }

    @Test
    @DisplayName("conserva los datos con los que se arma la carta")
    void conservaLosDatosDeLaCarta() {
        var dto = disponibilidadCon("DISPONIBLE", null);
        assertEquals("P-4", dto.getCodigo());
        assertEquals("Sopa", dto.getNombre());
        assertEquals("Entrada", dto.getCategoria());
    }

    @Test
    @DisplayName("un producto disponible no trae motivo")
    void elDisponibleNoTraeMotivo() {
        var dto = disponibilidadCon("DISPONIBLE", null);
        assertEquals("DISPONIBLE", dto.getEstado());
        assertNull(dto.getMotivo());
    }

    @Test
    @DisplayName("un producto agotado si explica por que")
    void elAgotadoExplicaPorQue() {
        var dto = disponibilidadCon("AGOTADO", "No alcanza el stock de 'Papa criolla'");
        assertNotNull(dto.getMotivo());
        assertFalse(dto.getMotivo().isBlank());
    }

    @Test
    @DisplayName("un producto sin receta tambien sale agotado, con su propio motivo")
    void elProductoSinRecetaSaleAgotado() {
        // Es la regla que descubrimos al cargar los datos de prueba: sin
        // receta el producto nunca aparece disponible.
        var dto = disponibilidadCon("AGOTADO", "El producto no tiene receta registrada");
        assertEquals("AGOTADO", dto.getEstado());
        assertTrue(dto.getMotivo().toLowerCase().contains("receta"));
    }

    @Test
    @DisplayName("los datos no cambian despues de creado")
    void losDatosNoCambian() {
        // Todos los campos son final: lo que responde la consulta se lee, no
        // se edita.
        var dto = disponibilidadCon("AGOTADO", "Sin insumos");
        assertEquals("AGOTADO", dto.getEstado());
        assertEquals("AGOTADO", dto.getEstado());
        assertEquals(4L, dto.getProductoId());
    }

    @Test
    @DisplayName("dos respuestas distintas no se pisan entre si")
    void dosRespuestasNoSePisan() {
        var unaDisponible = disponibilidadCon("DISPONIBLE", null);
        var unaAgotada = disponibilidadCon("AGOTADO", "Sin insumos");
        assertNotEquals(unaDisponible.getEstado(), unaAgotada.getEstado());
        assertNull(unaDisponible.getMotivo());
        assertNotNull(unaAgotada.getMotivo());
    }
}
