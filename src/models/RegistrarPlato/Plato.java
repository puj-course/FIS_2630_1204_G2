package RegistrarPlato;


public class Plato {
    // Declara la clase Plato.

    private long productoId;
    // Identificador único del producto o plato.

    private String codigo;
    // Código que identifica al plato.

    private String nombre;
    // Nombre del plato.

    private String descripcion;
    // Descripción del plato.

    private String categoria;
    // Categoría a la que pertenece el plato.

    private double precioVenta;
    // Precio al que se vende el plato al cliente.

    private double costo;
    // Costo que tiene para el negocio producir o adquirir el plato.

    private double stockActual;
    // Cantidad disponible actualmente en el inventario.

    private double stockMinimo;
    // Cantidad mínima que debería haber disponible.

    private double stockMaximo;
    // Cantidad máxima que se desea mantener en inventario.

    private String estado; // 'DISPONIBLE', 'AGOTADO'.
    // Indica el estado actual del plato.


    public Plato() {}
    // Constructor vacío.


    public Plato(String codigo, String nombre, String descripcion, String categoria,
                 double precioVenta, double costo) {
        // Constructor que permite crear un Plato

        this.codigo = codigo;
        // Guarda el código recibido en el atributo codigo.

        this.nombre = nombre;
        // Guarda el nombre recibido en el atributo nombre.

        this.descripcion = descripcion;
        // Guarda la descripción recibida en el atributo descripcion.

        this.categoria = categoria;
        // Guarda la categoría recibida en el atributo categoria.

        this.precioVenta = precioVenta;
        // Guarda el precio de venta recibido.

        this.costo = costo;
        // Guarda el costo recibido.
    }


    public long getProductoId() { return productoId; }
    // Getter: permite obtener el ID del producto.

    public void setProductoId(long productoId) { this.productoId = productoId; }
    // Setter: permite modificar el ID del producto.


    public String getCodigo() { return codigo; }
    // Getter: permite obtener el código del plato.

    public void setCodigo(String codigo) { this.codigo = codigo; }
    // Setter: permite modificar el código.


    public String getNombre() { return nombre; }
    // Getter: permite obtener el nombre del plato.

    public void setNombre(String nombre) { this.nombre = nombre; }
    // Setter: permite modificar el nombre.


    public String getDescripcion() { return descripcion; }
    // Getter: permite obtener la descripción.

    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    // Setter: permite modificar la descripción.


    public String getCategoria() { return categoria; }
    // Getter: permite obtener la categoría.

    public void setCategoria(String categoria) { this.categoria = categoria; }
    // Setter: permite modificar la categoría.


    public double getPrecioVenta() { return precioVenta; }
    // Getter: permite obtener el precio de venta.

    public void setPrecioVenta(double precioVenta) { this.precioVenta = precioVenta; }
    // Setter: permite modificar el precio de venta.


    public double getCosto() { return costo; }
    // Getter: permite obtener el costo del plato.

    public void setCosto(double costo) { this.costo = costo; }
    // Setter: permite modificar el costo.


    public double getStockActual() { return stockActual; }
    // Getter: permite obtener la cantidad actual disponible.

    public void setStockActual(double stockActual) { this.stockActual = stockActual; }
    // Setter: permite modificar el stock actual.


    public double getStockMinimo() { return stockMinimo; }
    // Getter: permite obtener el stock mínimo establecido.

    public void setStockMinimo(double stockMinimo) { this.stockMinimo = stockMinimo; }
    // Setter: permite modificar el stock mínimo.


    public double getStockMaximo() { return stockMaximo; }
    // Getter: permite obtener el stock máximo establecido.

    public void setStockMaximo(double stockMaximo) { this.stockMaximo = stockMaximo; }
    // Setter: permite modificar el stock máximo.


    public String getEstado() { return estado; }
    // Getter: permite obtener el estado del plato.

    public void setEstado(String estado) { this.estado = estado; }
    // Setter: permite modificar el estado.

}