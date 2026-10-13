package util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("HU-39 — buscador del menú del mesero")
class TextoBusquedaTest {

    @Nested
    @DisplayName("Ignora tildes y mayúsculas")
    class SinTildes {

        @Test
        @DisplayName("'aji' encuentra 'Ají picante'")
        void sinTildeEncuentraConTilde() {
            assertTrue(TextoBusqueda.coincide("Ají picante", null, "aji"));
        }

        @Test
        @DisplayName("'AJÍ' encuentra 'aji picante'")
        void conTildeEncuentraSinTilde() {
            assertTrue(TextoBusqueda.coincide("aji picante", null, "AJÍ"));
        }

        @Test
        @DisplayName("'limon' encuentra 'Limonada de coco'")
        void coincidenciaParcial() {
            assertTrue(TextoBusqueda.coincide("Limonada de coco", null, "limon"));
        }

        @Test
        @DisplayName("la eñe se trata como n en los dos sentidos")
        void enieEquivaleAEne() {
            assertTrue(TextoBusqueda.coincide("Jugo de piña", null, "pina"));
            assertTrue(TextoBusqueda.coincide("Jugo de pina", null, "PIÑA"));
        }
    }

    @Nested
    @DisplayName("Busca también en la descripción")
    class Descripcion {

        @Test
        @DisplayName("encuentra por un ingrediente que solo está en la descripción")
        void encuentraEnDescripcion() {
            assertTrue(TextoBusqueda.coincide("Bandeja paisa", "Carne de res molida", "res"));
        }

        @Test
        @DisplayName("una descripción nula no rompe la búsqueda")
        void descripcionNula() {
            assertFalse(TextoBusqueda.coincide("Gaseosa", null, "res"));
        }
    }

    @Nested
    @DisplayName("Búsqueda vacía")
    class Vacia {

        @Test
        @DisplayName("cadena vacía, espacios y null muestran todo el menú")
        void vaciaCoincideConTodo() {
            assertTrue(TextoBusqueda.coincide("Lo que sea", null, ""));
            assertTrue(TextoBusqueda.coincide("Lo que sea", null, "   "));
            assertTrue(TextoBusqueda.coincide("Lo que sea", null, null));
        }
    }

    @Test
    @DisplayName("no inventa coincidencias")
    void noCoincideCuandoNoDebe() {
        assertFalse(TextoBusqueda.coincide("Bandeja paisa", "Con chicharrón", "sushi"));
    }

    @Test
    @DisplayName("normalizar deja el texto en minúsculas y sin tildes")
    void normalizarQuitaTildes() {
        org.junit.jupiter.api.Assertions.assertEquals("aji", TextoBusqueda.normalizar("  ÁJÍ  "));
        org.junit.jupiter.api.Assertions.assertEquals("", TextoBusqueda.normalizar(null));
    }
}
