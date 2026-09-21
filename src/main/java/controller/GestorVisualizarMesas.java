package servicio;

import modelo.Cliente;
import modelo.Comanda;
import modelo.ListaEspera;
import modelo.Mesa;
import modelo.ResultadoCierreComanda;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Administra las mesas del restaurante: abrir/cerrar comandas, lista de
 * espera cuando no hay mesas libres, e inhabilitación temporal de mesas.
 */
public class GestorVisualizarMesas {

    private final List<Mesa> mesas = new ArrayList<>();
    private final Map<Integer, Comanda> comandasActivas = new HashMap<>();
    private final List<Comanda> historialComandas = new ArrayList<>();
    private final ListaEspera listaEspera = new ListaEspera();
    private int siguienteIdComanda = 1;

    public GestorMesas(int cantidadMesas) {
        for (int i = 1; i <= cantidadMesas; i++) {
            mesas.add(new Mesa(i));
        }
    }

    public List<Mesa> listarMesas() {
        return mesas;
    }

    public ListaEspera getListaEspera() {
        return listaEspera;
    }

    public Mesa buscarMesaPorNumero(int numero) {
        for (Mesa m : mesas) {
            if (m.getNumero() == numero) return m;
        }
        return null;
    }

    private Mesa buscarMesaDisponible() {
        for (Mesa m : mesas) {
            if (m.estaDisponible()) return m;
        }
        return null;
    }

    public Comanda abrirComanda(int numeroMesa) {
        Mesa mesa = buscarMesaPorNumero(numeroMesa);
        if (mesa == null) {
            throw new IllegalArgumentException("No existe la mesa " + numeroMesa + ".");
        }
        Comanda comanda = new Comanda(siguienteIdComanda++, mesa); // Mesa.ocupar() valida el estado
        comandasActivas.put(numeroMesa, comanda);
        return comanda;
    }

    public Comanda solicitarMesa(Cliente cliente) {
        Mesa mesaLibre = buscarMesaDisponible();
        if (mesaLibre != null) {
            return abrirComanda(mesaLibre.getNumero());
        }
        listaEspera.agregarCliente(cliente);
        return null;
    }

    public Comanda obtenerComandaActiva(int numeroMesa) {
        return comandasActivas.get(numeroMesa);
    }

    public ResultadoCierreComanda cerrarComanda(int numeroMesa, String metodoPago, double montoPagado) {
        Comanda comanda = comandasActivas.get(numeroMesa);
        if (comanda == null) {
            throw new IllegalStateException("La mesa " + numeroMesa + " no tiene una comanda abierta.");
        }
        comanda.cerrar(metodoPago, montoPagado);
        comandasActivas.remove(numeroMesa);
        historialComandas.add(comanda);

        if (!listaEspera.estaVacia()) {
            Cliente siguienteCliente = listaEspera.atenderSiguiente();
            Comanda nuevaComanda = abrirComanda(numeroMesa);
            return new ResultadoCierreComanda(comanda, siguienteCliente, nuevaComanda);
        }
        return new ResultadoCierreComanda(comanda);
    }

    public void inhabilitarMesa(int numeroMesa, String motivo) {
        Mesa mesa = buscarMesaPorNumero(numeroMesa);
        if (mesa == null) {
            throw new IllegalArgumentException("No existe la mesa " + numeroMesa + ".");
        }
        mesa.inhabilitar(motivo);
    }

    public void habilitarMesa(int numeroMesa) {
        Mesa mesa = buscarMesaPorNumero(numeroMesa);
        if (mesa == null) {
            throw new IllegalArgumentException("No existe la mesa " + numeroMesa + ".");
        }
        mesa.habilitar();
    }

    public List<Comanda> listarHistorialComandas() {
        return historialComandas;
    }
}
