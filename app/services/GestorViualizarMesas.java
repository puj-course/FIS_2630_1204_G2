
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Administra las mesas del restaurante: abrir/cerrar comandas, y ahora
 * también inhabilitar/habilitar mesas para sacarlas temporalmente de
 * servicio (mantenimiento, daño, reservas especiales, etc.).
 */
public class GestorVisualizarMesas {

    private final List<Mesa> mesas = new ArrayList<>();
    private final Map<Integer, Comanda> comandasActivas = new HashMap<>();
    private final List<Comanda> historialComandas = new ArrayList<>();
    private int siguienteIdComanda = 1;

    public GestorMesas(int cantidadMesas) {
        for (int i = 1; i <= cantidadMesas; i++) {
            mesas.add(new Mesa(i));
        }
    }

    public List<Mesa> listarMesas() {
        return mesas;
    }

    public Mesa buscarMesaPorNumero(int numero) {
        for (Mesa m : mesas) {
            if (m.getNumero() == numero) return m;
        }
        return null;
    }

    /**
     * Abre una comanda para una mesa. Falla si la mesa ya está ocupada
     * o si está fuera de servicio (la razón que motiva esta funcionalidad).
     */
    public Comanda abrirComanda(int numeroMesa) {
        Mesa mesa = buscarMesaPorNumero(numeroMesa);
        if (mesa == null) {
            throw new IllegalArgumentException("No existe la mesa " + numeroMesa + ".");
        }
        Comanda comanda = new Comanda(siguienteIdComanda++, mesa); // Mesa.ocupar() ya valida el estado
        comandasActivas.put(numeroMesa, comanda);
        return comanda;
    }

    public Comanda obtenerComandaActiva(int numeroMesa) {
        return comandasActivas.get(numeroMesa);
    }

    public Comanda cerrarComanda(int numeroMesa, String metodoPago, double montoPagado) {
        Comanda comanda = comandasActivas.get(numeroMesa);
        if (comanda == null) {
            throw new IllegalStateException("La mesa " + numeroMesa + " no tiene una comanda abierta.");
        }
        comanda.cerrar(metodoPago, montoPagado);
        comandasActivas.remove(numeroMesa);
        historialComandas.add(comanda);
        return comanda;
    }

    /**
     * Inhabilita temporalmente una mesa para que no se le asignen pedidos.
     */
    public void inhabilitarMesa(int numeroMesa, String motivo) {
        Mesa mesa = buscarMesaPorNumero(numeroMesa);
        if (mesa == null) {
            throw new IllegalArgumentException("No existe la mesa " + numeroMesa + ".");
        }
        mesa.inhabilitar(motivo);
    }

    /**
     * Vuelve a habilitar una mesa que estaba fuera de servicio.
     */
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
 