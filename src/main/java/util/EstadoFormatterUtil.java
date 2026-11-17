package util;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Traduce los códigos de estado que guarda la base de datos a las etiquetas
 * que ve el usuario en pantalla.
 *
 * Antes eran dos métodos con la misma estructura: la misma validación, la
 * misma normalización y una cadena de casos que solo se diferenciaba en los
 * textos. SonarQube los contaba como código duplicado, y con razón: cambiar
 * la forma de normalizar obligaba a tocar los dos sitios. Ahora cada estado
 * es una entrada de una tabla y la lógica común está escrita una vez.
 */
public class EstadoFormatterUtil {

    private static final String DESCONOCIDO = "Desconocido";

    private static final Map<String, String> ETIQUETAS_MESA = Map.ofEntries(
            Map.entry("DISPONIBLE", "Disponible"),
            Map.entry("LIBRE", "Disponible"),
            Map.entry("OCUPADA", "Ocupada"),
            Map.entry("RESERVADA", "Reservada"),
            Map.entry("PENDIENTE_PAGO", "Pendiente de pago"),
            Map.entry("PENDIENTE_DE_PAGO", "Pendiente de pago"),
            Map.entry("INHABILITADA", "Inhabilitada"),
            Map.entry("INHABILITADO", "Inhabilitada"),
            Map.entry("MANTENIMIENTO", "Inhabilitada"));

    private static final Map<String, String> ETIQUETAS_PEDIDO = Map.ofEntries(
            Map.entry("PENDIENTE", "Pendiente"),
            Map.entry("ASIGNADO_MESA", "Asignado a mesa"),
            Map.entry("EN_PREPARACION", "En preparación"),
            Map.entry("COMPLETADO", "Completado"),
            Map.entry("ENTREGADO", "Entregado"),
            Map.entry("CANCELADO", "Cancelado"));

    /**
     * Convierte el estado interno de una mesa en una etiqueta comprensible para la UI.
     *
     * Si el estado no está en la tabla, se devuelve en minúsculas, que es un
     * texto aceptable para mostrar aunque no se reconozca.
     */
    public static String formatearEstadoMesa(String codigoEstado) {
        return etiqueta(codigoEstado, ETIQUETAS_MESA, String::toLowerCase);
    }

    /**
     * Convierte el estado interno de un pedido en una etiqueta comprensible para la UI.
     *
     * Si el estado no está en la tabla, se devuelve tal cual llegó.
     */
    public static String formatearEstadoPedido(String estadoPedido) {
        return etiqueta(estadoPedido, ETIQUETAS_PEDIDO, UnaryOperator.identity());
    }

    /**
     * Busca la etiqueta del valor recibido.
     *
     * @param porDefecto qué hacer cuando el estado no está en la tabla; es lo
     *                   único que cambia entre mesas y pedidos
     */
    private static String etiqueta(String valor,
                                   Map<String, String> etiquetas,
                                   UnaryOperator<String> porDefecto) {

        if (valor == null || valor.isBlank()) {
            return DESCONOCIDO;
        }

        String normalizado = valor.trim()
                .toUpperCase()
                .replace(' ', '_')
                .replace('-', '_');

        String conocida = etiquetas.get(normalizado);

        return conocida != null ? conocida : porDefecto.apply(valor);
    }
}
