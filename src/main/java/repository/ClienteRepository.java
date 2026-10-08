package repository;

import ConexionBD.ConexionBD; //importa la conexión a la base de datos
import java.sql.Connection; // Importa Connection de Java SQL.
import java.sql.PreparedStatement; // Sirve para ejecutar consultas SQL de manera preparada y segura,
import java.sql.SQLException; // Es una excepción que puede ocurrir cuando hay un problema con la base de datos,
import entity.Cliente; //importa el modelo de cliente


public class ClienteRepository {

    // La tabla se llama "clientes", no "cliente" (HU-115). Ademas exige tipo y
    // numero de documento y apellido, los tres obligatorios: el INSERT anterior,
    // con solo nombre, telefono y correo, habria fallado siempre.
    //
    // El tipo de documento se resuelve por su codigo ('CC', 'CE', 'PA', 'NIT')
    // en lugar de recibir el id, porque el id depende del orden en que se haya
    // sembrado el catalogo.
    private static final String SQL_INSERTAR =
            "INSERT INTO clientes "
          + "(id_tipo_documento, numero_documento, nombre, apellido, correo, telefono) "
          + "SELECT td.id_tipo_documento, ?, ?, ?, ?, ? "
          + "  FROM tipos_documento td "
          + " WHERE td.codigo = ?";

    public void guardar(Cliente cliente) throws SQLException {

        try (Connection conn = ConexionBD.getConnection(); //conecta con la base de datos
             PreparedStatement stmt = conn.prepareStatement(SQL_INSERTAR)) {

            //coloca el contenido del cliente en el ?
            //el numero significa la posición en que va
            stmt.setString(1, cliente.getDocumento());
            stmt.setString(2, cliente.getNombre());
            stmt.setString(3, cliente.getApellido());
            stmt.setString(4, cliente.getCorreo());
            stmt.setString(5, cliente.getTelefono());
            stmt.setString(6, cliente.getTipoDocumento());

            if (stmt.executeUpdate() == 0) {
                // El SELECT no devolvio ninguna fila: el codigo de tipo de
                // documento no esta en el catalogo. Sin este aviso el cliente
                // simplemente no se guardaba y nadie se enteraba.
                throw new SQLException(
                        "No se guardo el cliente: el tipo de documento '"
                        + cliente.getTipoDocumento() + "' no existe en tipos_documento.");
            }
        }
    }

}
