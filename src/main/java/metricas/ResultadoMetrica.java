package metricas;

import java.util.List;
import java.util.Map;

/**
 * Resultado de una métrica, listo para imprimirse o guardarse en el reporte.
 *
 * @param nombre     nombre de la métrica
 * @param queMide    explicación de qué mide exactamente
 * @param resumen    valores agregados (etiqueta -> valor), en orden de inserción
 * @param detalle    líneas de detalle (por ejemplo, una por clase)
 * @param alertas    umbrales superados; vacía si todo está dentro de lo esperado
 */
public record ResultadoMetrica(String nombre,
                               String queMide,
                               Map<String, String> resumen,
                               List<String> detalle,
                               List<String> alertas) {

    /** Texto plano del resultado, en el formato del reporte. */
    public String aTexto() {
        StringBuilder sb = new StringBuilder();
        sb.append("== ").append(nombre).append(" ==\n");
        sb.append("Qué mide: ").append(queMide).append("\n\n");

        sb.append("Resumen\n");
        resumen.forEach((etiqueta, valor) ->
                sb.append("  - ").append(etiqueta).append(": ").append(valor).append('\n'));

        if (!detalle.isEmpty()) {
            sb.append("\nDetalle\n");
            detalle.forEach(linea -> sb.append("  ").append(linea).append('\n'));
        }

        sb.append("\nAlertas\n");
        if (alertas.isEmpty()) {
            sb.append("  (ninguna: todos los valores están dentro del umbral)\n");
        } else {
            alertas.forEach(a -> sb.append("  ! ").append(a).append('\n'));
        }
        return sb.toString();
    }
}
