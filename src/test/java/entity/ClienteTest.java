package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cliente es la persona que reserva o que pide factura. Tiene tres formas de
 * crearse segun cuanta informacion haya pedido el mesero.
 */
class ClienteTest {

    @Test
    void conservaElNombreYElTelefono() throws Exception {
        var c = new Cliente("Carlos Ruiz", "3001234567", "carlos@correo.com");
        assertEquals("Carlos Ruiz", c.getNombre());
        assertEquals("3001234567", c.getTelefono());
    }

    @Test
    @DisplayName("el constructor corto sirve para una reserva por telefono")
    void elConstructorCortoSirveParaReserva() {
        // Para apartar una mesa no hace falta el documento.
        var c = new Cliente("Carlos Ruiz", "3001234567", "carlos@correo.com");
        assertEquals("carlos@correo.com", c.getCorreo());
        assertNull(c.getDocumento());
        assertNull(c.getTipoDocumento());
    }

    @Test
    @DisplayName("el constructor completo sirve para facturar")
    void elConstructorCompletoSirveParaFacturar() {
        var c = new Cliente("CC", "1020304050", "Carlos", "Ruiz",
                "carlos@correo.com", "3001234567");
        assertEquals("CC", c.getTipoDocumento());
        assertEquals("1020304050", c.getDocumento());
        assertEquals("Carlos", c.getNombre());
        assertEquals("Ruiz", c.getApellido());
    }

    @Test
    @DisplayName("el constructor completo deja el correo y el telefono en su sitio")
    void elConstructorCompletoNoCruzaLosDatos() {
        // El orden de los parametros es nombre, apellido, correo, telefono:
        // vale la pena fijarlo porque es facil cruzarlos.
        var c = new Cliente("CC", "1020304050", "Carlos", "Ruiz",
                "carlos@correo.com", "3001234567");
        assertEquals("carlos@correo.com", c.getCorreo());
        assertEquals("3001234567", c.getTelefono());
    }

    @Test
    @DisplayName("el constructor vacio deja todo sin llenar")
    void elConstructorVacioDejaTodoSinLlenar() {
        var c = new Cliente();
        assertEquals(0, c.getId());
        assertNull(c.getNombre());
        assertNull(c.getApellido());
        assertNull(c.getDocumento());
    }

    @Test
    @DisplayName("el correo es opcional")
    void elCorreoEsOpcional() {
        var c = new Cliente("Carlos Ruiz", "3001234567", null);
        assertNull(c.getCorreo());
        assertNotNull(c.getTelefono());
    }

    @Test
    @DisplayName("se puede completar despues lo que falto al crearlo")
    void seCompletaDespues() {
        var c = new Cliente("Carlos Ruiz", "3001234567", null);
        c.setTipoDocumento("CC");
        c.setDocumento("1020304050");
        c.setApellido("Ruiz");
        assertEquals("CC", c.getTipoDocumento());
        assertEquals("1020304050", c.getDocumento());
        assertEquals("Ruiz", c.getApellido());
    }

    @Test
    @DisplayName("acepta los tipos de documento que maneja el restaurante")
    void aceptaLosTiposDeDocumento() {
        for (String tipo : new String[]{"CC", "CE", "NIT", "PA"}) {
            var c = new Cliente();
            c.setTipoDocumento(tipo);
            assertEquals(tipo, c.getTipoDocumento());
        }
    }

    @Test
    @DisplayName("al guardarlo la base le asigna el identificador")
    void alGuardarloRecibeIdentificador() {
        var c = new Cliente("Carlos Ruiz", "3001234567", null);
        c.setId(15);
        assertEquals(15, c.getId());
    }
}
