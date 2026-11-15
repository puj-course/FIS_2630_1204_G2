package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Zona es cada area del salon: terraza, segundo piso, salon principal. Las
 * mesas del mapa se agrupan por zona.
 */
class ZonaTest {

    @Test
    void conservaElNombreDeLaZona() throws Exception {
        var z = new Zona();
        z.setNombreZona("Terraza");
        assertEquals("Terraza", z.getNombreZona());
    }

    @Test
    @DisplayName("una zona nueva nace activa")
    void naceActiva() {
        // El indicador es un entero, no un booleano, porque en la base la
        // columna guarda 1 y 0.
        assertEquals(1, new Zona().getIsActive());
    }

    @Test
    @DisplayName("se desactiva poniendo el indicador en cero")
    void seDesactivaConCero() {
        var z = new Zona();
        z.setIsActive(0);
        assertEquals(0, z.getIsActive());
    }

    @Test
    @DisplayName("el indicador admite nulo porque la columna lo admite")
    void elIndicadorAdmiteNulo() {
        var z = new Zona();
        z.setIsActive(null);
        assertNull(z.getIsActive());
    }

    @Test
    @DisplayName("una zona nueva todavia no tiene identificador")
    void naceSinIdentificador() {
        assertNull(new Zona().getId());
    }

    @Test
    @DisplayName("guarda una descripcion para que el administrador la reconozca")
    void guardaLaDescripcion() {
        var z = new Zona();
        z.setNombreZona("Terraza");
        z.setDescripcion("Al aire libre, con vista a la calle");
        assertEquals("Al aire libre, con vista a la calle", z.getDescripcion());
    }

    @Test
    @DisplayName("la descripcion es opcional")
    void laDescripcionEsOpcional() {
        var z = new Zona();
        z.setNombreZona("Salon principal");
        assertNull(z.getDescripcion());
        assertNotNull(z.getNombreZona());
    }
}
