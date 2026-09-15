public class Modificacion {

    private TipoModificacion tipo;
    private Ingrediente ingredienteOriginal; // el que estaba en la receta (QUITAR, CAMBIAR, CAMBIAR_CANTIDAD)
    private Ingrediente ingredienteNuevo;    // el que reemplaza o se agrega (AGREGAR, CAMBIAR)
    private double cantidad;                 // cantidad nueva/agregada (según el tipo)

    public static Modificacion quitar(Ingrediente original) {
        Modificacion m = new Modificacion();
        m.tipo = TipoModificacion.QUITAR;
        m.ingredienteOriginal = original;
        return m;
    }

    public static Modificacion agregar(Ingrediente nuevo, double cantidad) {
        Modificacion m = new Modificacion();
        m.tipo = TipoModificacion.AGREGAR;
        m.ingredienteNuevo = nuevo;
        m.cantidad = cantidad;
        return m;
    }

    public static Modificacion cambiar(Ingrediente original, Ingrediente nuevo, double cantidad) {
        Modificacion m = new Modificacion();
        m.tipo = TipoModificacion.CAMBIAR;
        m.ingredienteOriginal = original;
        m.ingredienteNuevo = nuevo;
        m.cantidad = cantidad;
        return m;
    }

    public static Modificacion cambiarCantidad(Ingrediente original, double nuevaCantidad) {
        Modificacion m = new Modificacion();
        m.tipo = TipoModificacion.CAMBIAR_CANTIDAD;
        m.ingredienteOriginal = original;
        m.cantidad = nuevaCantidad;
        return m;
    }

    public TipoModificacion getTipo() {
        return tipo;
    }

    @Override
    public String toString() {
        switch (tipo) {
            case QUITAR:
                return "Sin " + ingredienteOriginal.getNombre();
            case AGREGAR:
                return "Extra " + ingredienteNuevo.getNombre() + " (" + cantidad + " " + ingredienteNuevo.getUnidadMedida() + ")";
            case CAMBIAR:
                return "Cambiar " + ingredienteOriginal.getNombre() + " por " + ingredienteNuevo.getNombre() +
                        " (" + cantidad + " " + ingredienteNuevo.getUnidadMedida() + ")";
            case CAMBIAR_CANTIDAD:
                return "Ajustar " + ingredienteOriginal.getNombre() + " a " + cantidad + " " + ingredienteOriginal.getUnidadMedida();
            default:
                return "";
        }
    }
}
