package models;

import modelo.Ingrediente;
import modelo.PedidoPersonalizado;
import modelo.Producto;
import modelo.Receta;
import modelo.RecetaDetalle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reemplaza a la base de datos: guarda todo en listas/mapas en memoria.
 * Se pierde al cerrar el programa (es solo para probar la funcionalidad).
 */
public class GestorRecetas {

    private final List<Producto> productos = new ArrayList<>();
    private final List<Ingrediente> ingredientes = new ArrayList<>();
    private final Map<Integer, Receta> recetasPorProducto = new HashMap<>(); // idProducto -> Receta

    public GestorRecetas() {
        cargarDatosDePrueba();
    }

    private void cargarDatosDePrueba() {
        productos.add(new Producto(1, "Hamburguesa Clásica"));
        productos.add(new Producto(2, "Pizza Margarita"));
        productos.add(new Producto(3, "Limonada"));
        productos.add(new Producto(4, "Pato a la naranja"));

        ingredientes.add(new Ingrediente(10, "Pan brioche", "unidad"));
        ingredientes.add(new Ingrediente(11, "Carne de res", "gr"));
        ingredientes.add(new Ingrediente(12, "Queso mozzarella", "gr"));
        ingredientes.add(new Ingrediente(13, "Lechuga", "gr"));
        ingredientes.add(new Ingrediente(14, "Tomate", "gr"));
        ingredientes.add(new Ingrediente(15, "Masa de pizza", "unidad"));
        ingredientes.add(new Ingrediente(16, "Salsa de tomate", "ml"));
        ingredientes.add(new Ingrediente(17, "Limón", "unidad"));
        ingredientes.add(new Ingrediente(18, "Azúcar", "gr"));
        ingredientes.add(new Ingrediente(19, "Papa", "gr"));
        ingredientes.add(new Ingrediente(20, "Plátano", "gr"));
        ingredientes.add(new Ingrediente(21, "Cebolla", "gr"));
        ingredientes.add(new Ingrediente(22, "Pechuga de pato", "gr"));
        ingredientes.add(new Ingrediente(23, "Salsa de naranja", "ml"));

        // Receta precargada, para poder probar la personalización de inmediato
        Receta recetaPato = new Receta(buscarProductoPorId(4));
        recetaPato.agregarIngrediente(buscarIngredientePorId(22), 200); // pechuga de pato
        recetaPato.agregarIngrediente(buscarIngredientePorId(19), 150); // papa
        recetaPato.agregarIngrediente(buscarIngredientePorId(21), 40);  // cebolla
        recetaPato.agregarIngrediente(buscarIngredientePorId(23), 50);  // salsa de naranja
        recetasPorProducto.put(4, recetaPato);
    }

    public List<Producto> listarProductos() {
        return productos;
    }

    public List<Ingrediente> listarIngredientes() {
        return ingredientes;
    }

    public Producto buscarProductoPorId(int id) {
        for (Producto p : productos) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    public Ingrediente buscarIngredientePorId(int id) {
        for (Ingrediente i : ingredientes) {
            if (i.getId() == id) return i;
        }
        return null;
    }

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
}
 