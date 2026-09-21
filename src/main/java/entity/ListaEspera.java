package models;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Administra el orden de llegada de los clientes que están esperando
 * mesa. Es una cola FIFO: el primero en llegar es el primero en ser
 * acomodado cuando se libera una mesa.
 */
public class ListaEspera {

    private final Queue<Cliente> espera = new LinkedList<>();

    /**
     * Agrega un cliente al final de la lista de espera.
     *
     * @return la posición en la que quedó (1 = el siguiente en ser atendido)
     */
    public int agregarCliente(Cliente cliente) {
        espera.add(cliente);
        return espera.size();
    }

    /**
     * Consulta quién sigue en la fila, sin sacarlo de la lista.
     */
    public Cliente verSiguiente() {
        return espera.peek();
    }

    /**
     * Saca de la fila al primer cliente (el que lleva más tiempo esperando)
     * para acomodarlo en una mesa recién liberada.
     */
    public Cliente atenderSiguiente() {
        if (espera.isEmpty()) {
            throw new IllegalStateException("No hay clientes en la lista de espera.");
        }
        return espera.poll();
    }

    public boolean estaVacia() {
        return espera.isEmpty();
    }

    public int cantidadEnEspera() {
        return espera.size();
    }

    /**
     * Devuelve la posición de un cliente en la fila (1 = el siguiente),
     * o -1 si no está en la lista.
     */
    public int posicionDe(int idCliente) {
        int posicion = 1;
        for (Cliente c : espera) {
            if (c.getId() == idCliente) return posicion;
            posicion++;
        }
        return -1;
    }

    public List<Cliente> listar() {
        return new LinkedList<>(espera);
    }
}
 