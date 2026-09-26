package entity; // Indica que esta clase pertenece al paquete "RegistroInventario".

// Declara la clase EntradaInventario.
// Esta clase representa una entrada de un ingrediente al inventario.
public class EntradaInventario {

    // Identificador único del movimiento de inventario.
    private int idMovimiento;

    // Guarda el ID del ingrediente al que corresponde esta entrada.
    private Long ingredienteId;

    // Indica qué tipo de movimiento se está realizando.
    // En esta clase será "entrada".
    private String tipoMovimiento; // 'entrada', 'salida', etc.

    // Cantidad del ingrediente que está entrando al inventario.
    private double cantidad;

    // Guarda el motivo por el cual se realiza la entrada.
    private String motivo;

    public EntradaInventario() {}

    // Constructor que permite crear una entrada proporcionando:
    // - El ID del ingrediente.
    // - La cantidad que entra.
    // - El motivo de la entrada.
    public EntradaInventario(Long ingredienteId, double cantidad, String motivo) {
        this.ingredienteId = ingredienteId;
        // Guarda el ID del ingrediente recibido en el atributo ingredienteId.

        this.cantidad = cantidad;
        // Guarda la cantidad recibida en el atributo cantidad.

        this.motivo = motivo;
        // Guarda el motivo recibido en el atributo motivo.

        this.tipoMovimiento = "entrada";
        // Establece automáticamente el tipo de movimiento como "entrada".
        // No es necesario que el usuario lo indique.
        // Esta clase está diseñada específicamente para registrar entradas.
    }

    public int getIdMovimiento() { return idMovimiento; }
    // Getter: permite obtener el valor del ID del movimiento.

    public void setIdMovimiento(int idMovimiento) { this.idMovimiento = idMovimiento; }
    // Setter: permite modificar el ID del movimiento.


    public Long getIngredienteId() { return ingredienteId; }
    // Getter: permite obtener el ID del ingrediente.

    public void setIngredienteId(Long ingredienteId) { this.ingredienteId = ingredienteId; }
    // Setter: permite modificar el ID del ingrediente.


    public String getTipoMovimiento() { return tipoMovimiento; }
    // Getter: permite obtener el tipo de movimiento.

    public void setTipoMovimiento(String tipoMovimiento) { this.tipoMovimiento = tipoMovimiento; }
    // Setter: permite modificar el tipo de movimiento.


    public double getCantidad() { return cantidad; }
    // Getter: permite obtener la cantidad de la entrada.

    public void setCantidad(double cantidad) { this.cantidad = cantidad; }
    // Setter: permite modificar la cantidad.


    public String getMotivo() { return motivo; }
    // Getter: permite obtener el motivo de la entrada.

    public void setMotivo(String motivo) { this.motivo = motivo; }
    // Setter: permite modificar el motivo.

}
