package com.gastroflow.session;

public final class SesionUsuario {

    private static Integer usuarioId;
    private static String nombre;
    private static String rol;

    private SesionUsuario() {
    }

    public static void iniciarSesion(int id, String nombreUsuario, String nombreRol) {
        usuarioId = id;
        nombre = nombreUsuario;
        rol = nombreRol;
    }

    public static void cerrarSesion() {
        usuarioId = null;
        nombre = null;
        rol = null;
    }

    public static boolean estaAutenticado() {
        return usuarioId != null;
    }

    public static boolean esCajero() {
        return rol != null && rol.equalsIgnoreCase("CAJERO");
    }

    public static int getUsuarioId() {
        if (usuarioId == null) {
            throw new IllegalStateException("No hay un usuario autenticado.");
        }
        return usuarioId;
    }

    public static String getNombre() {
        return nombre == null ? "-" : nombre;
    }

    public static String getRol() {
        return rol == null ? "-" : rol;
    }
}
