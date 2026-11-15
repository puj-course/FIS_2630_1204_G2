package support;

import ConexionBD.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mockito.MockedStatic;

import static org.mockito.Mockito.*;

public final class JdbcSimulado implements AutoCloseable {
    public final Connection conexion = mock(Connection.class);
    public final PreparedStatement sentencia = mock(PreparedStatement.class);
    public final ResultSet filas = mock(ResultSet.class);
    private final MockedStatic<ConexionBD> acceso;

    public JdbcSimulado() throws SQLException {
        when(conexion.prepareStatement(anyString())).thenReturn(sentencia);
        when(sentencia.executeQuery()).thenReturn(filas);
        when(sentencia.executeUpdate()).thenReturn(1);
        acceso = mockStatic(ConexionBD.class);
        acceso.when(ConexionBD::getConnection).thenReturn(conexion);
    }

    @Override
    public void close() {
        acceso.close();
    }
}

