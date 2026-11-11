package metricas;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Métrica 3 — Tamaño de las clases.
 */
public class MetricaTamanioClases implements Metrica {

    static final int UMBRAL_NOA = 15;
    static final int UMBRAL_NOM = 30;
    static final int UMBRAL_PARAMETROS = 4;
    private static final int TOP = 5;

    /** Tamaño de una clase y su método con más parámetros. */
    public record FilaTamanio(Class<?> clase, int noa, int nom, int maxParametros,
                              int sumaParametros, int miembrosConParametros) { }

    @Override
    public String nombre() {
        return "Tamaño de clases (atributos, métodos y parámetros)";
    }

    @Override
    public ResultadoMetrica medir(List<Class<?>> clasesDelProyecto) {
        List<String> noMedidas = new ArrayList<>();
        List<FilaTamanio> filas = calcular(clasesDelProyecto, noMedidas);

        double promNoa = filas.stream().mapToInt(FilaTamanio::noa).average().orElse(0);
        double promNom = filas.stream().mapToInt(FilaTamanio::nom).average().orElse(0);
        int totalMiembros = filas.stream().mapToInt(FilaTamanio::miembrosConParametros).sum();
        double promParametros = totalMiembros == 0 ? 0
                : (double) filas.stream().mapToInt(FilaTamanio::sumaParametros).sum() / totalMiembros;
        int maxParametros = filas.stream().mapToInt(FilaTamanio::maxParametros).max().orElse(0);

        Map<String, String> resumen = new LinkedHashMap<>();
        resumen.put("Clases evaluadas", String.valueOf(filas.size()));
        resumen.put("NOA promedio / máximo", formato(promNoa) + " / "
                + filas.stream().mapToInt(FilaTamanio::noa).max().orElse(0));
        resumen.put("NOM promedio / máximo", formato(promNom) + " / "
                + filas.stream().mapToInt(FilaTamanio::nom).max().orElse(0));
        resumen.put("Parámetros por método (promedio / máximo)", formato(promParametros) + " / " + maxParametros);
        resumen.put("Clases con NOA > " + UMBRAL_NOA,
                String.valueOf(filas.stream().filter(f -> f.noa() > UMBRAL_NOA).count()));
        resumen.put("Clases con NOM > " + UMBRAL_NOM,
                String.valueOf(filas.stream().filter(f -> f.nom() > UMBRAL_NOM).count()));
        resumen.put("Clases con algún método de más de " + UMBRAL_PARAMETROS + " parámetros",
                String.valueOf(filas.stream().filter(f -> f.maxParametros() > UMBRAL_PARAMETROS).count()));

        List<String> detalle = new ArrayList<>();
        detalle.add("Clases con más atributos (NOA):");
        filas.stream().sorted(Comparator.comparingInt(FilaTamanio::noa).reversed()
                        .thenComparing(f -> f.clase().getName()))
                .limit(TOP)
                .forEach(f -> detalle.add(String.format(Locale.ROOT, "  %-45s NOA=%d",
                        f.clase().getName(), f.noa())));
        detalle.add("Clases con más métodos (NOM):");
        filas.stream().sorted(Comparator.comparingInt(FilaTamanio::nom).reversed()
                        .thenComparing(f -> f.clase().getName()))
                .limit(TOP)
                .forEach(f -> detalle.add(String.format(Locale.ROOT, "  %-45s NOM=%d",
                        f.clase().getName(), f.nom())));
        detalle.add("Clases con el método de más parámetros:");
        filas.stream().sorted(Comparator.comparingInt(FilaTamanio::maxParametros).reversed()
                        .thenComparing(f -> f.clase().getName()))
                .limit(TOP)
                .forEach(f -> detalle.add(String.format(Locale.ROOT, "  %-45s máx. parámetros=%d",
                        f.clase().getName(), f.maxParametros())));
        noMedidas.forEach(n -> detalle.add("No se pudo medir: " + n));

        List<String> alertas = new ArrayList<>();
        for (FilaTamanio f : filas) {
            if (f.noa() > UMBRAL_NOA) {
                alertas.add(f.clase().getName() + ": NOA " + f.noa() + " supera el umbral de " + UMBRAL_NOA);
            }
            if (f.nom() > UMBRAL_NOM) {
                alertas.add(f.clase().getName() + ": NOM " + f.nom() + " supera el umbral de " + UMBRAL_NOM);
            }
            if (f.maxParametros() > UMBRAL_PARAMETROS) {
                alertas.add(f.clase().getName() + ": un método recibe " + f.maxParametros()
                        + " parámetros (umbral " + UMBRAL_PARAMETROS + ")");
            }
        }

        return new ResultadoMetrica(
                nombre(),
                "Cuántos atributos (NOA) y métodos (NOM) declara cada clase, y cuántos parámetros "
                        + "recibe cada método o constructor. Sirve para detectar clases que concentran "
                        + "demasiadas responsabilidades.",
                resumen, detalle, alertas);
    }

    /** Calcula el tamaño de cada clase. Visible para las pruebas. */
    List<FilaTamanio> calcular(List<Class<?>> clases, List<String> noMedidas) {
        List<FilaTamanio> filas = new ArrayList<>();
        for (Class<?> clase : clases) {
            if (clase.isInterface() || clase.isEnum() || clase.isAnnotation()) {
                continue;
            }
            try {
                int noa = (int) java.util.Arrays.stream(clase.getDeclaredFields())
                        .filter(c -> !c.isSynthetic() && !Modifier.isStatic(c.getModifiers()))
                        .count();

                List<Integer> parametros = new ArrayList<>();
                int nom = 0;
                for (Method metodo : clase.getDeclaredMethods()) {
                    if (!metodo.isSynthetic()) {
                        nom++;
                        parametros.add(metodo.getParameterCount());
                    }
                }
                for (Constructor<?> constructor : clase.getDeclaredConstructors()) {
                    if (!constructor.isSynthetic()) {
                        parametros.add(constructor.getParameterCount());
                    }
                }

                filas.add(new FilaTamanio(clase, noa, nom,
                        parametros.stream().mapToInt(Integer::intValue).max().orElse(0),
                        parametros.stream().mapToInt(Integer::intValue).sum(),
                        parametros.size()));
            } catch (LinkageError e) {
                noMedidas.add(clase.getName() + " (" + e.getClass().getSimpleName() + ")");
            }
        }
        return filas;
    }

    private static String formato(double valor) {
        return String.format(Locale.ROOT, "%.2f", valor);
    }
}
