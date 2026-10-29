package util;

public class EstadoFormatterUtil {

    /**
     * Convierte el estado interno de una mesa en una etiqueta comprensible para la UI.
     */
    public static String formatearEstadoMesa(String codigoEstado) {
        if (codigoEstado == null || codigoEstado.isBlank()) {
            return "Desconocido";
        }

        String normalizado = codigoEstado.trim().toUpperCase().replace(' ', '_').replace('-', '_');

        switch (normalizado) {
            case "DISPONIBLE", "LIBRE" -> {
                return "Disponible";
            }
            case "OCUPADA" -> {
                return "Ocupada";
            }
            case "RESERVADA" -> {
                return "Reservada";
            }
            case "PENDIENTE_PAGO", "PENDIENTE_DE_PAGO" -> {
                return "Pendiente de pago";
            }
            case "INHABILITADA", "INHABILITADO", "MANTENIMIENTO" -> {
                return "Inhabilitada";
            }
            default -> {
                // Tratamiento seguro para estados desconocidos: devuelve el texto capitalizado o limpio
                return codigoEstado.toLowerCase();
            }
        }
    }

    /**
     * Convierte el estado interno de un pedido en una etiqueta comprensible para la UI.
     */
    public static String formatearEstadoPedido(String estadoPedido) {
        if (estadoPedido == null || estadoPedido.isBlank()) {
            return "Desconocido";
        }

        String normalizado = estadoPedido.trim().toUpperCase().replace(' ', '_').replace('-', '_');

        switch (normalizado) {
            case "PENDIENTE" -> {
                return "Pendiente";
            }
            case "ASIGNADO_MESA" -> {
                return "Asignado a mesa";
            }
            case "EN_PREPARACION" -> {
                return "En preparación";
            }
            case "COMPLETADO" -> {
                return "Completado";
            }
            case "ENTREGADO" -> {
                return "Entregado";
            }
            case "CANCELADO" -> {
                return "Cancelado";
            }
            default -> {
                return estadoPedido;
            }
        }
    }
}