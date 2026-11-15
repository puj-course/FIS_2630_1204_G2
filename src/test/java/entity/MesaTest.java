package entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mesa es cada cuadro del mapa del salon. Lo que se ve escrito encima sale de
 * su toString, que prefiere el codigo y cae al numero cuando no hay codigo.
 */
class MesaTest {

    @Test
    void conservaElNumeroYLaCapacidad() throws Exception {
        var mesa = new Mesa();
        mesa.setNumeroMesa(12);
        mesa.setCapacidad(4);
        assertEquals(12, mesa.getNumeroMesa());
        assertEquals(4, mesa.getCapacidad());
    }

    // ------------------------------------------- lo que se ve en el mapa --

    @Test
    @DisplayName("se muestra con el codigo cuando lo tiene")
    void seMuestraConElCodigo() {
        var mesa = new Mesa();
        mesa.setNumeroMesa(12);
        mesa.setCodigoMesa("M-012");
        assertEquals("Mesa M-012", mesa.toString());
    }

    @Test
    @DisplayName("sin codigo se muestra con el numero")
    void sinCodigoSeMuestraConElNumero() {
        var mesa = new Mesa();
        mesa.setNumeroMesa(12);
        assertEquals("Mesa 12", mesa.toString());
    }

    @Test
    @DisplayName("un codigo en blanco cuenta como si no hubiera codigo")
    void elCodigoEnBlancoNoCuenta() {
        var mesa = new Mesa();
        mesa.setNumeroMesa(7);
        mesa.setCodigoMesa("   ");
        assertEquals("Mesa 7", mesa.toString());

        mesa.setCodigoMesa("");
        assertEquals("Mesa 7", mesa.toString());
    }

    @Test
    @DisplayName("una mesa recien creada se muestra como Mesa 0")
    void unaMesaNuevaSeMuestraComoCero() {
        // Los campos numericos son int, no Integer, asi que arrancan en cero
        // en vez de quedar nulos.
        assertEquals("Mesa 0", new Mesa().toString());
    }

    @Test
    @DisplayName("siempre devuelve algo para pintar, nunca nulo")
    void siempreDevuelveAlgoParaPintar() {
        assertNotNull(new Mesa().toString());
        assertFalse(new Mesa().toString().isBlank());
    }

    // ---------------------------------------------- datos de la ocupacion --

    @Test
    @DisplayName("una mesa libre no tiene comensales ni pedido activo")
    void unaMesaLibreNoTieneOcupacion() {
        var mesa = new Mesa();
        assertNull(mesa.getCantidadComensales());
        assertNull(mesa.getPedidoActivo());
        assertNull(mesa.getMeseroResponsable());
    }

    @Test
    @DisplayName("una mesa ocupada guarda cuantos son y quien los atiende")
    void unaMesaOcupadaGuardaLaOcupacion() {
        var mesa = new Mesa();
        mesa.setCantidadComensales(3);
        mesa.setPedidoActivo("PED-0007");
        mesa.setMeseroResponsable("Laura Gomez");
        assertEquals(3, mesa.getCantidadComensales());
        assertEquals("PED-0007", mesa.getPedidoActivo());
        assertEquals("Laura Gomez", mesa.getMeseroResponsable());
    }

    @Test
    @DisplayName("al liberar la mesa los datos de ocupacion vuelven a quedar nulos")
    void alLiberarSeLimpiaLaOcupacion() {
        var mesa = new Mesa();
        mesa.setCantidadComensales(3);
        mesa.setPedidoActivo("PED-0007");

        mesa.setCantidadComensales(null);
        mesa.setPedidoActivo(null);
        assertNull(mesa.getCantidadComensales());
        assertNull(mesa.getPedidoActivo());
    }

    @Test
    @DisplayName("guarda a que zona pertenece, con el nombre que se ve en pantalla")
    void guardaLaZona() {
        var mesa = new Mesa();
        mesa.setIdZona(2);
        mesa.setNombreZona("Terraza");
        assertEquals(2, mesa.getIdZona());
        assertEquals("Terraza", mesa.getNombreZona());
    }

    @Test
    @DisplayName("guarda el estado por codigo y por identificador")
    void guardaElEstadoDeLasDosFormas() {
        // El identificador es la llave foranea y el codigo es lo que se usa
        // para pintar el color.
        var mesa = new Mesa();
        mesa.setIdEstadoMesa(1);
        mesa.setCodigoEstado("DISPONIBLE");
        assertEquals(1, mesa.getIdEstadoMesa());
        assertEquals("DISPONIBLE", mesa.getCodigoEstado());
    }
}
