package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NotaMesa es la nota que el mesero le deja a una mesa: un cumpleanos, una
 * alergia, que el cliente pidio la cuenta.
 */
class NotaMesaTest {

    private NotaMesa notaCon(int idMesa, String texto) {
        var n = new NotaMesa();
        n.setIdMesa(idMesa);
        n.setTextoNota(texto);
        return n;
    }

    @Test
    void conservaElTextoDeLaNota() throws Exception {
        assertEquals("Cliente alergico al mani", notaCon(4, "Cliente alergico al mani").getTextoNota());
    }

    @Test
    @DisplayName("la nota sabe a que mesa pertenece")
    void sabeAQueMesaPertenece() {
        assertEquals(4, notaCon(4, "x").getIdMesa());
    }

    @Test
    @DisplayName("una nota nueva no trae fecha hasta que se guarda")
    void naceSinFecha() {
        // A diferencia de otras entidades, esta no se pone la fecha sola: la
        // pone la base al insertar.
        assertNull(new NotaMesa().getFechaCreacion());
    }

    @Test
    @DisplayName("al leerla de la base trae la fecha en que se escribio")
    void alLeerlaTraeLaFecha() {
        var cuando = LocalDateTime.of(2026, 11, 10, 20, 15);
        var n = notaCon(4, "Cumpleanos en la mesa");
        n.setFechaCreacion(cuando);
        assertEquals(cuando, n.getFechaCreacion());
    }

    @Test
    @DisplayName("una nota nueva arranca con los identificadores en cero")
    void arrancaEnCero() {
        // Son int, no Integer, asi que no quedan nulos.
        var n = new NotaMesa();
        assertEquals(0, n.getIdNota());
        assertEquals(0, n.getIdMesa());
    }

    @Test
    @DisplayName("acepta una nota larga sin recortarla")
    void aceptaNotaLarga() {
        var larga = "El cliente pidio que la bandeja venga sin chicharron y con "
                + "el huevo aparte, y que la gaseosa llegue al final";
        assertEquals(larga, notaCon(4, larga).getTextoNota());
    }

    @Test
    @DisplayName("acepta quedarse sin texto, aunque no sirva de nada")
    void aceptaTextoNulo() {
        assertNull(notaCon(4, null).getTextoNota());
    }
}
