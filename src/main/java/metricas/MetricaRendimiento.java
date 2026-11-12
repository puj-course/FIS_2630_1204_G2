package metricas;

import service.descuento.CalculadoraDescuento;
import service.descuento.DescuentoMontoFijo;
import service.descuento.DescuentoPorcentaje;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Métrica 4 — Rendimiento del cálculo de descuentos.
 */
public class MetricaRendimiento implements Metrica {

    /** Una operación en memoria que tarda más de esto está mal. */
    static final double UMBRAL_P95_MICROSEGUNDOS = 1000.0;

    private static final BigDecimal SUBTOTAL = new BigDecimal("100000");

    private final int calentamiento;
    private final int mediciones;

    /** Evita que la JVM elimine los cálculos por considerarlos inútiles. */
    private volatile int sumidero;

    public MetricaRendimiento() {
        this(50_000, 200_000); // valores por defecto, que dan resultados estables en una máquina moderna
    }

    /** Constructor con tamaños configurables (las pruebas usan valores pequeños). */
    public MetricaRendimiento(int calentamiento, int mediciones) {
        if (calentamiento < 0 || mediciones <= 0) {
            throw new IllegalArgumentException("El calentamiento no puede ser negativo y las mediciones deben ser mayores a 0");
        }
        this.calentamiento = calentamiento;
        this.mediciones = mediciones;
    }

    /** Resultado numérico de medir una estrategia. */
    public record Medicion(String estrategia, double promedioMicros, double p95Micros,
                           double maximoMicros, double operacionesPorSegundo) { }

    @Override
    public String nombre() { 
        return "Rendimiento (latencia y throughput de CalculadoraDescuento)";
    }

    @Override
    public ResultadoMetrica medir(List<Class<?>> clasesDelProyecto) {
        List<Medicion> resultados = List.of(
                medirEstrategia("Porcentaje (%)", new CalculadoraDescuento(new DescuentoPorcentaje()),
                        new BigDecimal("10")),
                medirEstrategia("Monto fijo ($)", new CalculadoraDescuento(new DescuentoMontoFijo()),
                        new BigDecimal("5000")));

        Map<String, String> resumen = new LinkedHashMap<>();
        resumen.put("Calentamiento / mediciones por estrategia", calentamiento + " / " + mediciones);
        List<String> detalle = new ArrayList<>();
        List<String> alertas = new ArrayList<>();

        for (Medicion m : resultados) {
            resumen.put(m.estrategia() + " — promedio / p95 / máximo (µs)",
                    String.format(Locale.ROOT, "%.3f / %.3f / %.3f",
                            m.promedioMicros(), m.p95Micros(), m.maximoMicros())); // promedio, p95 y máximo de latencia
            resumen.put(m.estrategia() + " — throughput", // cálculos por segundo
                    String.format(Locale.ROOT, "%,.0f cálculos/s", m.operacionesPorSegundo()));
            if (m.p95Micros() > UMBRAL_P95_MICROSEGUNDOS) {
                alertas.add(String.format(Locale.ROOT,
                        "%s: la latencia p95 (%.3f µs) supera el umbral de %.0f µs",
                        m.estrategia(), m.p95Micros(), UMBRAL_P95_MICROSEGUNDOS));
            }
        }
        detalle.add("La latencia incluye el costo de leer el reloj (unos 20-50 ns por medición).");
        detalle.add("Es un micro-benchmark en memoria: no incluye base de datos ni interfaz.");

        return new ResultadoMetrica(
                nombre(),
                "Cuánto tarda en promedio un cálculo de descuento, el tiempo que no supera el 95 % "
                        + "de las llamadas (p95) y cuántos cálculos completa por segundo.",
                resumen, detalle, alertas);
    }

    /**
     * Mide una estrategia: primero la latencia de cada llamada, luego el throughput.
     *
     * Cada bucle acumula en una variable local y escribe el sumidero una sola vez
     * al final. Sumar directamente sobre el campo volátil tenía dos problemas: la
     * suma sobre un campo volátil no es atómica, y además metía una escritura
     * volátil dentro del tramo cronometrado, que es justo lo que se está midiendo.
     */
    Medicion medirEstrategia(String nombre, CalculadoraDescuento calculadora, BigDecimal valor) {
        int acumulado = sumidero;

        for (int i = 0; i < calentamiento; i++) {
            acumulado += calculadora.calcular(SUBTOTAL, valor).signum();
        }
        sumidero = acumulado;

        long[] latencias = new long[mediciones];
        for (int i = 0; i < mediciones; i++) {
            long inicio = System.nanoTime();
            acumulado += calculadora.calcular(SUBTOTAL, valor).signum();
            latencias[i] = System.nanoTime() - inicio;
        }
        sumidero = acumulado;

        long inicioTotal = System.nanoTime();
        for (int i = 0; i < mediciones; i++) {
            acumulado += calculadora.calcular(SUBTOTAL, valor).signum();
        }
        double segundos = Math.max(System.nanoTime() - inicioTotal, 1) / 1_000_000_000.0;
        sumidero = acumulado;

        Arrays.sort(latencias);
        double promedio = Arrays.stream(latencias).average().orElse(0) / 1000.0;
        return new Medicion(nombre, promedio, percentil(latencias, 95) / 1000.0,
                latencias[latencias.length - 1] / 1000.0, mediciones / segundos);
    }

    /** Percentil por el método del rango más cercano. El arreglo debe venir ordenado. */
    static double percentil(long[] ordenado, double porcentaje) {
        if (ordenado.length == 0) {
            throw new IllegalArgumentException("No hay mediciones para calcular el percentil");
        }
        int indice = (int) Math.ceil(porcentaje / 100.0 * ordenado.length) - 1;
        return ordenado[Math.max(0, Math.min(indice, ordenado.length - 1))];
    }
}
