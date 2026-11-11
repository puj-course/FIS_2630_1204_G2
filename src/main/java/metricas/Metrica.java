package metricas;

import java.util.List;

public interface Metrica {

    String nombre();

    ResultadoMetrica medir(List<Class<?>> clasesDelProyecto);
}
