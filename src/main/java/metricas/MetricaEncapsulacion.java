package metricas;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Métrica 2 — Encapsulación.
 *
 * Mide qué porcentaje de los atributos del proyecto está declarado
 * es decir, oculto detrás de los métodos de su clase.
 */
public class MetricaEncapsulacion implements Metrica {

    static final double UMBRAL_INDICE = 90.0; //criterio en el que minimo cumpliría con la métrica

    public record FilaEncapsulacion(Class<?> clase, int evaluados, int privados,  // atributos evaluados y privados
                                    int noPrivados, int publicosMutables) { }

    @Override
    public String nombre() {
        return "Encapsulación (% de atributos privados)";
    }
    
    @Override
    public ResultadoMetrica medir(List<Class<?>> clasesDelProyecto) { // visible para las pruebas
        List<String> noMedidas = new ArrayList<>(); // clases que no se pudieron medir
        List<FilaEncapsulacion> filas = calcular(clasesDelProyecto, noMedidas);
        // Se calculan los totales y el índice de encapsulación
        int evaluados = filas.stream().mapToInt(FilaEncapsulacion::evaluados).sum();
        int privados = filas.stream().mapToInt(FilaEncapsulacion::privados).sum();
        int noPrivados = filas.stream().mapToInt(FilaEncapsulacion::noPrivados).sum();
        int publicosMutables = filas.stream().mapToInt(FilaEncapsulacion::publicosMutables).sum();
        double indice = evaluados == 0 ? 100.0 : 100.0 * privados / evaluados;
        long clasesConExposicion = filas.stream().filter(f -> f.noPrivados() > 0).count();

        Map<String, String> resumen = new LinkedHashMap<>(); // se usa LinkedHashMap para mantener el orden de inserción
        resumen.put("Clases evaluadas", String.valueOf(filas.size()));
        resumen.put("Atributos evaluados (sin constantes)", String.valueOf(evaluados));
        resumen.put("Atributos private", String.valueOf(privados));
        resumen.put("Atributos no private", String.valueOf(noPrivados));
        resumen.put("Atributos public no final (los más riesgosos)", String.valueOf(publicosMutables));
        resumen.put("Índice de encapsulación", String.format(Locale.ROOT, "%.2f %%", indice));
        resumen.put("Clases con algún atributo no private", String.valueOf(clasesConExposicion));

        List<String> detalle = new ArrayList<>();
        filas.stream()
                .filter(f -> f.noPrivados() > 0)
                .sorted(Comparator.comparingInt(FilaEncapsulacion::noPrivados).reversed()
                        .thenComparing(f -> f.clase().getName()))
                .forEach(f -> detalle.add(String.format(Locale.ROOT,
                        "%-45s no private=%d (public no final=%d) de %d atributos",
                        f.clase().getName(), f.noPrivados(), f.publicosMutables(), f.evaluados())));
        noMedidas.forEach(n -> detalle.add("No se pudo medir: " + n));

        List<String> alertas = new ArrayList<>();
        if (indice < UMBRAL_INDICE) {
            alertas.add(String.format(Locale.ROOT,
                    "El índice de encapsulación (%.2f %%) está por debajo del umbral de %.0f %%",
                    indice, UMBRAL_INDICE));
        }
        filas.stream()
                .filter(f -> f.publicosMutables() > 0)
                .forEach(f -> alertas.add(f.clase().getName() + " tiene "
                        + f.publicosMutables() + " atributo(s) public no final"));

        return new ResultadoMetrica(
                nombre(),
                "Porcentaje de atributos declarados private respecto al total de atributos "
                        + "(excluyendo constantes). Un valor alto significa que el estado de cada clase "
                        + "solo se modifica a través de sus propios métodos, donde se pueden validar "
                        + "las reglas.",
                resumen, detalle, alertas);
    }

    /** Calcula la encapsulación de cada clase. Visible para las pruebas. */
    List<FilaEncapsulacion> calcular(List<Class<?>> clases, List<String> noMedidas) {
        List<FilaEncapsulacion> filas = new ArrayList<>();
        for (Class<?> clase : clases) {
            if (!esClaseNormal(clase)) {
                continue;
            }
            try {
                int evaluados = 0;
                int privados = 0;
                int publicosMutables = 0;
                for (Field campo : clase.getDeclaredFields()) {
                    if (campo.isSynthetic() || esConstante(campo)) {
                        continue;
                    }
                    evaluados++;
                    int mod = campo.getModifiers();
                    if (Modifier.isPrivate(mod)) {
                        privados++;
                    } else if (Modifier.isPublic(mod) && !Modifier.isFinal(mod)) {
                        publicosMutables++;
                    }
                }
                filas.add(new FilaEncapsulacion(clase, evaluados, privados, evaluados - privados, publicosMutables));
            } catch (LinkageError e) {
                noMedidas.add(clase.getName() + " (" + e.getClass().getSimpleName() + ")");
            }
        }
        return filas;
    }

    private static boolean esConstante(Field campo) {
        int mod = campo.getModifiers();
        return Modifier.isStatic(mod) && Modifier.isFinal(mod);
    }

    private static boolean esClaseNormal(Class<?> clase) {
        return !clase.isInterface() && !clase.isEnum() && !clase.isAnnotation();
    }
}
