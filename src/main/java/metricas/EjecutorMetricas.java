package metricas;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


public final class EjecutorMetricas {

    private static final Path REPORTE = Path.of("target", "metricas", "reporte-metricas.txt"); // ruta del archivo de salida

    private EjecutorMetricas() { }

    public static void main(String[] args) throws IOException {
        Path raiz = Path.of(args.length > 0 ? args[0] : "target/classes"); // ruta de la carpeta donde están las clases compiladas del proyecto
        String reporte = generarReporte(raiz, metricasDisponibles()); // se genera el reporte de métricas


        PrintStream salida = new PrintStream(System.out, true, StandardCharsets.UTF_8); // se asegura que la salida sea UTF-8
        salida.println(reporte);
        Files.createDirectories(REPORTE.getParent());
        Files.writeString(REPORTE, reporte, StandardCharsets.UTF_8);
        salida.println("Reporte guardado en " + REPORTE);
    }

    /** Aquí se van registrando las métricas a medida que se implementan. */
    static List<Metrica> metricasDisponibles() { 
        return List.of(
                new MetricaEncapsulacion(),
                new MetricaTamanioClases(),
                new MetricaRendimiento());
    }

    static String generarReporte(Path raizClases, List<Metrica> metricas) throws IOException { // visible para las pruebas
        EscanerClases.Escaneo escaneo = EscanerClases.cargar(raizClases);

        StringBuilder sb = new StringBuilder(); // se usa StringBuilder para construir el reporte de manera eficiente
        sb.append("REPORTE DE MÉTRICAS DE CALIDAD (propias, distintas de SonarQube)\n");
        sb.append("Clases analizadas: ").append(escaneo.clases().size()).append("\n");
        if (!escaneo.omitidas().isEmpty()) {
            sb.append("Clases que no se pudieron cargar (").append(escaneo.omitidas().size()).append("):\n");
            escaneo.omitidas().forEach(o -> sb.append("  - ").append(o).append('\n'));
        }
        sb.append('\n');

        for (Metrica metrica : metricas) {
            sb.append(metrica.medir(escaneo.clases()).aTexto()).append('\n');
        }
        return sb.toString();
    }
}
