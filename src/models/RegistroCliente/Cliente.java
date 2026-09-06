package RegistroCliente;


public class Cliente {
// Declara la clase Cliente.
// Esta clase representa a un cliente dentro del sistema.


    private int id;
    // Identificador del cliente.
    // Normalmente este valor puede ser asignado por la base de datos.


    private String tipoDocumento; // "CC", "CE", etc.
    // Indica el tipo de documento del cliente.
    // Por ejemplo: "CC" (Cédula de Ciudadanía) o "CE" (Cédula de Extranjería).


    private String documento;
    // Guarda el número de documento del cliente.


    private String nombre;
    // Guarda el nombre del cliente.


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


    public int getId() { return id; }
    // Getter del ID.


    public void setId(int id) { this.id = id; }
    // Setter del ID.


    public String getNombre() { return nombre; }
    // Getter del nombre.


    public void setNombre(String nombre) { this.nombre = nombre; }
    // Setter del nombre.

    public String getTelefono() { return telefono; }
    // Getter del teléfono.


    public void setTelefono(String telefono) { this.telefono = telefono; }
    // Setter del teléfono.


    public String getCorreo() { return correo; }
    // Getter del correo.


    public void setCorreo(String correo) { this.correo = correo; }
    // Setter del correo.
}