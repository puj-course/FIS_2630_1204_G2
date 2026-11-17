package repository;

import support.JdbcSimulado;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

class UsuarioRepositoryTest {

    @Test
    @DisplayName("devuelve un Optional vacío cuando el identificador de usuario no se encuentra registrado")
    void devuelveResultadoVacioCuandoNoHayFilas() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Explicitamos que el ResultSet no encuentra registros
            when(jdbc.filas.next()).thenReturn(false);

            // Etapa de assert
            assertTrue(new UsuarioRepository().buscarPorIdentificador(" M4 ").isEmpty());

            // Verificaciones de interacciones con JDBC (valida limpieza/trim si aplica)
            verify(jdbc.sentencia).setString(1, "M4");
            verify(jdbc.sentencia).setString(2, "M4");
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }

    @Test
    @DisplayName("devuelve el usuario mapeado correctamente cuando el identificador existe en la base de datos")
    void devuelveUsuarioCuandoExisteRegistro() throws Exception {
        try (var jdbc = new JdbcSimulado()) {
            // Simulamos que el ResultSet encuentra un registro
            when(jdbc.filas.next()).thenReturn(true);

            // Configuramos los mocks de manera flexible para lectura por nombre de columna o índice
            when(jdbc.filas.getLong(anyString())).thenReturn(1L);
            when(jdbc.filas.getLong(anyInt())).thenReturn(1L);

            when(jdbc.filas.getString(anyString())).thenReturn("M4");
            when(jdbc.filas.getString(anyInt())).thenReturn("M4");

            var resultado = new UsuarioRepository().buscarPorIdentificador("M4");

            // Etapa de assert
            assertTrue(resultado.isPresent());
            var usuario = resultado.get();
            assertNotNull(usuario);

            // Verificaciones de interacciones con JDBC
            verify(jdbc.sentencia).setString(1, "M4");
            verify(jdbc.sentencia).setString(2, "M4");
            verify(jdbc.sentencia).executeQuery();
            verify(jdbc.sentencia).close();
        }
    }
}