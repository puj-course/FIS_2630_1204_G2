package com.gastroflow.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normalizacion de texto para el buscador del menu (HU-39).
 *
 * Esta en una clase aparte para poder probarla sin levantar JavaFX ni la base
 * de datos.
 */
public final class TextoBusqueda {

    private static final Locale ES_CO = Locale.of("es", "CO");

    private TextoBusqueda() {
    }

    /**
     * Pasa a minusculas y quita las tildes, para que "Aji" encuentre "Ají".
     * La enie tambien pierde la tilde, asi que "pina" encuentra "piña" y al
     * reves: en un restaurante nadie escribe tildes de afan.
     */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }

        return Normalizer.normalize(texto.trim().toLowerCase(ES_CO), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    /**
     * Busca el texto en el nombre y en la descripcion del producto.
     * Una busqueda vacia coincide con todo.
     */
    public static boolean coincide(String nombre, String descripcion, String busqueda) {
        String buscado = normalizar(busqueda);

        if (buscado.isEmpty()) {
            return true;
        }

        return normalizar(nombre).contains(buscado)
                || normalizar(descripcion).contains(buscado);
    }
}
