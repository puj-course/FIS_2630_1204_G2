import model.Cliente;
import model.Comanda;
import model.EstadoMesa;
import model.Mesa;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Genera la vista consolidada del estado de todas las mesas, para que
 * el personal identifique de un vistazo qué mesas están libres, cuáles
 * están ocupadas (y con qué comanda/consumo), cuáles están fuera de
 * servicio, y cuántos clientes están en espera.
 *
 * Es "en tiempo real" porque cada llamada consulta directamente el
 * estado actual de GestorMesas (no un dato guardado ni cacheado):
 * cualquier cambio (abrir, cerrar, inhabilitar, etc.) se refleja de
 * inmediato en la siguiente consulta.
 */
public class PanelMesas {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final GestorMesas gestorMesas;

    public PanelMesas(GestorMesas gestorMesas) {
        this.gestorMesas = gestorMesas;
    }

    /**
     * Genera el texto del panel con el estado actual de todas las mesas.
     */
    public String generarPanel() {
        StringBuilder sb = new StringBuilder();

        int disponibles = 0, ocupadas = 0, fueraDeServicio = 0;

        sb.append("=========== ESTADO DE MESAS - ").append(LocalDateTime.now().format(FORMATO_HORA)).append(" ===========\n");
        sb.append(String.format("%-8s %-18s %-40s%n", "Mesa", "Estado", "Detalle"));
        sb.append("-------------------------------------------------------------------------\n");

        for (Mesa mesa : gestorMesas.listarMesas()) {
            String detalle = generarDetalle(mesa);
            sb.append(String.format("%-8d %-18s %-40s%n", mesa.getNumero(), mesa.getEstado(), detalle));

            switch (mesa.getEstado()) {
                case DISPONIBLE -> disponibles++;
                case OCUPADA -> ocupadas++;
                case FUERA_DE_SERVICIO -> fueraDeServicio++;
            }
        }

        sb.append("-------------------------------------------------------------------------\n");
        sb.append(String.format("Total: %d  |  Disponibles: %d  |  Ocupadas: %d  |  Fuera de servicio: %d%n",
                gestorMesas.listarMesas().size(), disponibles, ocupadas, fueraDeServicio));

        sb.append(generarResumenListaEspera());
        sb.append("===========================================================================\n");

        return sb.toString();
    }

    private String generarDetalle(Mesa mesa) {
        switch (mesa.getEstado()) {
            case OCUPADA -> {
                Comanda comanda = gestorMesas.obtenerComandaActiva(mesa.getNumero());
                if (comanda == null) return "-";
                return "Comanda #" + comanda.getId() + " - Consumo actual: $"
                        + String.format("%,.0f", comanda.getMontoTotal());
            }
            case FUERA_DE_SERVICIO -> {
                return "Motivo: " + mesa.getMotivoFueraDeServicio();
            }
            default -> {
                return "-";
            }
        }
    }

    private String generarResumenListaEspera() {
        int enEspera = gestorMesas.getListaEspera().cantidadEnEspera();
        if (enEspera == 0) {
            return "Lista de espera: vacía.\n";
        }
        Cliente siguiente = gestorMesas.getListaEspera().verSiguiente();
        return "Lista de espera: " + enEspera + " cliente(s). Siguiente en la fila: " + siguiente + "\n";
    }

    /**
     * Genera y muestra el panel directamente por consola.
     */
    public void mostrarPanel() {
        System.out.println(generarPanel());
    }
}
 