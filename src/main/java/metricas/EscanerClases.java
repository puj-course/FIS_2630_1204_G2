package metricas;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class EscanerClases {

    private static final String PAQUETE_PROPIO = "metricas.";

    /**
     * @param clases   clases cargadas correctamente
     * @param omitidas clases que no se pudieron cargar (con el motivo)
     */
    public record Escaneo(List<Class<?>> clases, List<String> omitidas) { }

    private EscanerClases() { }

    public static Escaneo cargar(Path raizClases) throws IOException { // se carga el árbol de clases a partir de la raíz de clases
        List<Class<?>> clases = new ArrayList<>();
        List<String> omitidas = new ArrayList<>();

        try (URLClassLoader cargador = new URLClassLoader(
                new URL[]{raizClases.toUri().toURL()}, ClassLoader.getSystemClassLoader()); // se crea un cargador de clases que apunta a la raíz de clases
             Stream<Path> archivos = Files.walk(raizClases)) { // se recorre el árbol de directorios a partir de la raíz

            List<Path> rutas = archivos // se filtran los archivos que terminan en .class y se ordenan alfabéticamente
                    .filter(p -> p.toString().endsWith(".class"))
                    .sorted()
                    .toList();

            for (Path ruta : rutas) { // se obtiene el nombre de la clase a partir de la ruta del archivo
                String nombre = nombreDeClase(raizClases, ruta);
                if (esOmitible(nombre)) { // se omiten las clases internas, module-info y package-info, y las propias de la métrica
                    continue;
                }
                try {
                    clases.add(Class.forName(nombre, false, cargador)); // se carga la clase sin inicializarla (para evitar ejecutar código estático)
                } catch (LinkageError | ClassNotFoundException e) {
                    omitidas.add(nombre + " (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")"); // se registra la clase que no se pudo cargar con el motivo
                }
            }
        }
        return new Escaneo(clases, omitidas);
    }

    private static String nombreDeClase(Path raiz, Path archivo) { // se obtiene el nombre de la clase a partir de la ruta del archivo
        String relativa = raiz.relativize(archivo).toString();
        return relativa
                .substring(0, relativa.length() - ".class".length())
                .replace('/', '.')
                .replace('\\', '.');
    }

    private static boolean esOmitible(String nombre) { // se omiten las clases internas, module-info y package-info, y las propias de la métrica
        return nombre.contains("$")
                || nombre.endsWith("module-info")
                || nombre.endsWith("package-info")
                || nombre.startsWith(PAQUETE_PROPIO);
    }
}
