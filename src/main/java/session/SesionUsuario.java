package session;

/**
 * Patron Singleton: hay una sola sesion abierta a la vez y toda la aplicacion
 * la consulta por SesionUsuario.getInstance().
 *
 * Todo el estado y todos los metodos son de instancia. Antes el identificador
 * del usuario era un campo estatico que escribian metodos de instancia, lo que
 * mezclaba las dos formas de acceso sin ninguna ganancia.
 */
public final class SesionUsuario {

    /** La unica instancia de la clase. */
    private static final SesionUsuario INSTANCIA = new SesionUsuario();

    private Integer usuarioId;
    private String nombre;
    private String rol;

    private SesionUsuario() {
    }

    public static SesionUsuario getInstance() {
        return INSTANCIA;
    }

    public void iniciarSesion(int id, String nombreUsuario, String nombreRol) {
        usuarioId = id;
        nombre = nombreUsuario;
        rol = nombreRol;
    }

    public void cerrarSesion() {
        usuarioId = null;
        nombre = null;
        rol = null;
    }

    public boolean estaAutenticado() {
        return usuarioId != null;
    }

    public boolean esCajero() {
        return rol != null && rol.equalsIgnoreCase("CAJERO");
    }

    public int getUsuarioId() {
        if (usuarioId == null) {
            throw new IllegalStateException("No hay un usuario autenticado.");
        }
        return usuarioId;
    }

    public String getNombre() {
        return nombre == null ? "-" : nombre;
    }

    public String getRol() {
        return rol == null ? "-" : rol;
    }
}
