package entity;


public class Cliente {
// Declara la clase Cliente.
// Esta clase representa a un cliente dentro del sistema.


    private int id;
    // Identificador del cliente.
    // Normalmente este valor puede ser asignado por la base de datos.


    private String tipoDocumento; // "CC", "CE", etc.
    // Indica el tipo de documento del cliente.
    // Por ejemplo: "CC" (Cédula de Ciudadanía) o "CE" (Cédula de Extranjería).
    // Debe coincidir con tipos_documento.codigo en la base de datos.


    private String documento;
    // Guarda el número de documento del cliente.


    private String nombre;
    // Guarda el nombre del cliente.


    private String apellido;
    // Guarda el apellido del cliente.
    // La tabla clientes lo exige, igual que el nombre.


    private String telefono;
    // Guarda el número de teléfono del cliente.


    private String correo; // opcional
    // Guarda el correo electrónico del cliente.


    public Cliente() {}
    // Constructor vacío.


    public Cliente(String nombre, String telefono, String correo) {
        // Constructor que permite crear un Cliente

        this.nombre = nombre;
        // Guarda el nombre recibido en el atributo nombre.

        this.telefono = telefono;
        // Guarda el teléfono recibido en el atributo telefono.

        this.correo = correo;
        // Guarda el correo recibido en el atributo correo.
    }


    public Cliente(String tipoDocumento, String documento, String nombre,
                   String apellido, String correo, String telefono) {
        // Constructor con todo lo que la tabla clientes necesita para guardar.

        this.tipoDocumento = tipoDocumento;
        this.documento = documento;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.telefono = telefono;
    }


    public int getId() { return id; }
    // Getter del ID.


    public void setId(int id) { this.id = id; }
    // Setter del ID.


    public String getTipoDocumento() { return tipoDocumento; }
    // Getter del tipo de documento.


    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
    // Setter del tipo de documento.


    public String getDocumento() { return documento; }
    // Getter del número de documento.


    public void setDocumento(String documento) { this.documento = documento; }
    // Setter del número de documento.


    public String getNombre() { return nombre; }
    // Getter del nombre.


    public void setNombre(String nombre) { this.nombre = nombre; }
    // Setter del nombre.


    public String getApellido() { return apellido; }
    // Getter del apellido.


    public void setApellido(String apellido) { this.apellido = apellido; }
    // Setter del apellido.


    public String getTelefono() { return telefono; }
    // Getter del teléfono.


    public void setTelefono(String telefono) { this.telefono = telefono; }
    // Setter del teléfono.


    public String getCorreo() { return correo; }
    // Getter del correo.


    public void setCorreo(String correo) { this.correo = correo; }
    // Setter del correo.
}
