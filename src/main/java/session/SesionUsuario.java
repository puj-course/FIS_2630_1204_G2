package session;

public final class SesionUsuario {
    //La única instancia de la clase
    private static final SesionUsuario INSTANCIA = new SesionUsuario();

    // Estado de la sesión: ahora es de instancia, ya no estático.
    private  Integer usuarioId;
    private  String nombre;
    private  String rol;

    private SesionUsuario() {
    }

    public static SesionUsuario getInstance() {
        return INSTANCIA;
    }

    public  void iniciarSesion(int id, String nombreUsuario, String nombreRol) {
        usuarioId = id;
        nombre = nombreUsuario;
        rol = nombreRol;
    }

    public  void cerrarSesion() {
        usuarioId = null;
        nombre = null;
        rol = null;
    }

    public  boolean estaAutenticado() {
        return usuarioId != null;
    }

    public  boolean esCajero() {
        return rol != null && rol.equalsIgnoreCase("CAJERO");
    }

    public  int getUsuarioId() {
        if (usuarioId == null) {
            throw new IllegalStateException("No hay un usuario autenticado.");
        }
        return usuarioId;
    }

    public  String getNombre() {
        return nombre == null ? "-" : nombre;
    }

    public  String getRol() {
        return rol == null ? "-" : rol;
    }
}
