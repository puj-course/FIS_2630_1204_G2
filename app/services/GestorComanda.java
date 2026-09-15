import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Administra las mesas del restaurante y sus comandas: quién tiene una
 * comanda abierta, y aplica el cierre (pago + liberación de mesa).
 */
public class GestorComanda {

    private final List<Mesa> mesas = new ArrayList<>();
    private final Map<Integer, Comanda> comandasActivas = new HashMap<>(); // numeroMesa -> comanda ABIERTA
    private final List<Comanda> historialComandas = new ArrayList<>();     // comandas ya CERRADAS
    private int siguienteIdComanda = 1;

    public GestorComanda(int cantidadMesas) {
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
     * Abre una comanda nueva para una mesa disponible (marca la mesa como ocupada).
     */
    public Comanda abrirComanda(int numeroMesa) {
        Mesa mesa = buscarMesaPorNumero(numeroMesa);
        if (mesa == null) {
            throw new IllegalArgumentException("No existe la mesa " + numeroMesa + ".");
        }
        if (!mesa.estaDisponible()) {
            throw new IllegalStateException("La mesa " + numeroMesa + " ya está ocupada.");
        }
        Comanda comanda = new Comanda(siguienteIdComanda++, mesa);
        comandasActivas.put(numeroMesa, comanda);
        return comanda;
    }

    /**
     * Devuelve la comanda abierta de una mesa, o null si no tiene ninguna.
     */
    public Comanda obtenerComandaActiva(int numeroMesa) {
        return comandasActivas.get(numeroMesa);
    }

    /**
     * Cierra la comanda de una mesa (una vez pagado el pedido) y libera la mesa.
     *
     * @return la comanda ya cerrada, con el recibo listo para mostrar
     */
    public Comanda cerrarComanda(int numeroMesa, String metodoPago, double montoPagado) {
        Comanda comanda = comandasActivas.get(numeroMesa);
        if (comanda == null) {
            throw new IllegalStateException("La mesa " + numeroMesa + " no tiene una comanda abierta.");
        }

        comanda.cerrar(metodoPago, montoPagado); // valida el pago y libera la mesa

        comandasActivas.remove(numeroMesa);
        historialComandas.add(comanda);
        return comanda;
    }

    public List<Comanda> listarHistorialComandas() {
        return historialComandas;
    }
}
 