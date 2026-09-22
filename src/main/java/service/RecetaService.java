package servicio;

import modelo.Cuenta;
import modelo.Ingrediente;
import modelo.ItemPedido;
import modelo.PedidoPersonalizado;
import modelo.Producto;
import modelo.Receta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reemplaza a la base de datos: guarda todo en listas/mapas en memoria.
 * Se pierde al cerrar el programa (es solo mientras no tengas la BD conectada).
 *
 * No carga datos de prueba: productos, ingredientes y recetas se van
 * agregando a través de sus propios métodos. También administra la
 * cuenta del pedido (los productos ya confirmados) y genera la precuenta.
 */
public class RecetaService{

    private final List<Producto> productos = new ArrayList<>();
    private final List<Ingrediente> ingredientes = new ArrayList<>();
    private final Map<Integer, Receta> recetasPorProducto = new HashMap<>(); // idProducto -> Receta
    private final Cuenta cuenta = new Cuenta(); // productos ya confirmados del pedido

    public GestorRecetas() {
    }

    // ---------------- Productos ----------------

    public List<Producto> listarProductos() {
        return productos;
    }

    public Producto buscarProductoPorId(int id) {
        for (Producto p : productos) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    public void agregarProducto(Producto producto) {
        if (producto == null) {
            throw new IllegalArgumentException("El producto no puede ser nulo.");
        }
        if (buscarProductoPorId(producto.getId()) != null) {
            throw new IllegalArgumentException("Ya existe un producto con el id " + producto.getId() + ".");
        }
        productos.add(producto);
    }

    // ---------------- Ingredientes ----------------

    public List<Ingrediente> listarIngredientes() {
        return ingredientes;
    }

    public Ingrediente buscarIngredientePorId(int id) {
        for (Ingrediente i : ingredientes) {
            if (i.getId() == id) return i;
        }
        return null;
    }

    public void agregarIngrediente(Ingrediente ingrediente) {
        if (ingrediente == null) {
            throw new IllegalArgumentException("El ingrediente no puede ser nulo.");
        }
        if (buscarIngredientePorId(ingrediente.getId()) != null) {
            throw new IllegalArgumentException("Ya existe un ingrediente con el id " + ingrediente.getId() + ".");
        }
        ingredientes.add(ingrediente);
    }

    // ---------------- Recetas ----------------

    /**
     * Crea (o reemplaza) la receta de un producto.
     */
    public void guardarReceta(Receta receta) {
        if (!receta.tieneIngredientes()) {
            throw new IllegalArgumentException("La receta debe tener al menos un ingrediente.");
        }
        recetasPorProducto.put(receta.getProducto().getId(), receta);
    }

    public Receta obtenerRecetaDe(int idProducto) {
        return recetasPorProducto.get(idProducto);
    }

    public boolean tieneReceta(int idProducto) {
        return recetasPorProducto.containsKey(idProducto);
    }

    // ---------------- Pedidos personalizados ----------------

    /**
     * Crea un pedido personalizable a partir de la receta base de un producto.
     * La receta original NO se modifica: el pedido trabaja sobre una copia.
     */
    public PedidoPersonalizado iniciarPedido(int idProducto) {
        Receta receta = obtenerRecetaDe(idProducto);
        if (receta == null) {
            throw new IllegalStateException("El producto no tiene una receta creada todavía.");
        }
        return new PedidoPersonalizado(receta);
    }

    // ---------------- Cuenta / Precuenta ----------------

    /**
     * Confirma un plato ya personalizado (o sin cambios) dentro de la cuenta
     * del pedido, indicando cuántas unidades pidió el cliente.
     */
    public ItemPedido agregarItemACuenta(PedidoPersonalizado pedido, int cantidadUnidades) {
        ItemPedido item = new ItemPedido(pedido, cantidadUnidades);
        cuenta.agregarItem(item);
        return item;
    }

    public List<ItemPedido> listarItemsDeLaCuenta() {
        return cuenta.getItems();
    }

    public boolean cuentaEstaVacia() {
        return cuenta.estaVacia();
    }

    /**
     * Suma el subtotal de todos los productos confirmados en la cuenta.
     */
    public double calcularTotalCuenta() {
        return cuenta.calcularTotal();
    }

    /**
     * Genera el texto de la precuenta: producto, cantidad, precio unitario,
     * subtotal por línea, notas de personalización, y el total a pagar.
     */
    public String generarPrecuenta() {
        return cuenta.generarPrecuenta();
    }

    /**
     * Genera y muestra la precuenta directamente por consola.
     */
    public void mostrarPrecuenta() {
        cuenta.mostrarPrecuenta();
    }
}
