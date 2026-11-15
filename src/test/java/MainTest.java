import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {
    @Test
    void incluyeLasVistasDeLosModulosEnElClasspath() throws Exception {
        var modulos = Main.class.getDeclaredField("MODULOS");
        modulos.setAccessible(true);
        var entradas = (java.util.List<?>) modulos.get(null);
        assertFalse(entradas.isEmpty());
        for (var entrada : entradas) {
            var fxml = entrada.getClass().getDeclaredMethod("fxml");
            fxml.setAccessible(true);
            String ruta = (String) fxml.invoke(entrada);
            assertNotNull(Main.class.getResource(ruta), ruta);
        }
        assertNotNull(Main.class.getResource("/views/login.fxml"));
    }
}

