package service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Envía mensajes a Telegram desde la aplicación.
 *
 * Las credenciales no van escritas aquí. Se leen, en este orden:
 *
 *   1. Las variables de entorno TELEGRAM_BOT_TOKEN y TELEGRAM_CHAT_ID.
 *   2. El archivo conf/telegram.properties, con las claves telegram.token y
 *      telegram.chat.
 *
 * Es el mismo criterio que usa ConexionBD para la base de datos. El archivo
 * está excluido del repositorio: se copia de conf/telegram.properties.example
 * y cada quien escribe el suyo.
 *
 * Si no hay credenciales, el envío no se intenta: se deja un aviso en el
 * registro y se devuelve false. La aplicación sigue funcionando.
 */
public final class TelegramService {

    private static final Logger LOG = Logger.getLogger(TelegramService.class.getName());

    private static final String ARCHIVO_CONFIGURACION = "conf/telegram.properties";

    private static final Properties CONFIGURACION = cargarConfiguracion();

    /**
     * Un solo cliente para toda la aplicación.
     *
     * HttpClient está pensado para reutilizarse: mantiene su propio grupo de
     * conexiones y de hilos. Crear uno en cada envío deja esos recursos sin
     * liberar, que es justo lo que reportaba SonarQube. Cerrarlo tampoco
     * servía aquí: close() espera a que terminen las peticiones pendientes, y
     * eso convertiría en síncrono un método que existe para ser asíncrono.
     */
    private static final HttpClient CLIENTE = HttpClient.newHttpClient();

    private TelegramService() {
        // Clase de utilidades: no se instancia.
    }

    /**
     * Envía un mensaje al chat configurado.
     *
     * @return una promesa que vale true si Telegram respondió 200
     */
    public static CompletableFuture<Boolean> enviarMensajeAsync(String mensaje) {
        String token = valor("TELEGRAM_BOT_TOKEN", "telegram.token");
        String chat = valor("TELEGRAM_CHAT_ID", "telegram.chat");

        if (token == null || chat == null) {
            LOG.warning("Telegram no esta configurado. Defina TELEGRAM_BOT_TOKEN y "
                    + "TELEGRAM_CHAT_ID, o cree " + ARCHIVO_CONFIGURACION
                    + " a partir de " + ARCHIVO_CONFIGURACION + ".example");
            return CompletableFuture.completedFuture(false);
        }

        String cuerpo = "chat_id=" + URLEncoder.encode(chat, StandardCharsets.UTF_8)
                + "&text=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8);

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(URI.create("https://api.telegram.org/bot" + token + "/sendMessage"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();

        return CLIENTE.sendAsync(peticion, HttpResponse.BodyHandlers.ofString())
                .thenApply(respuesta -> respuesta.statusCode() == 200)
                .exceptionally(error -> {
                    LOG.log(Level.WARNING, "No se pudo enviar el mensaje a Telegram", error);
                    return false;
                });
    }

    /**
     * Indica si hay credenciales disponibles, para que la interfaz pueda
     * deshabilitar el botón en vez de ofrecer algo que va a fallar.
     */
    public static boolean estaConfigurado() {
        return valor("TELEGRAM_BOT_TOKEN", "telegram.token") != null
                && valor("TELEGRAM_CHAT_ID", "telegram.chat") != null;
    }

    /** Busca el dato primero en el entorno y después en el archivo. */
    private static String valor(String variableDeEntorno, String clave) {
        String delEntorno = System.getenv(variableDeEntorno);

        if (delEntorno != null && !delEntorno.isBlank()) {
            return delEntorno;
        }

        String delArchivo = CONFIGURACION.getProperty(clave);

        return delArchivo == null || delArchivo.isBlank() ? null : delArchivo;
    }

    /** Lee conf/telegram.properties si existe; si no, se sigue sin él. */
    private static Properties cargarConfiguracion() {
        Properties propiedades = new Properties();
        Path ruta = Path.of(ARCHIVO_CONFIGURACION);

        if (!Files.exists(ruta)) {
            return propiedades;
        }

        try (InputStream entrada = Files.newInputStream(ruta)) {
            propiedades.load(entrada);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "No se pudo leer " + ARCHIVO_CONFIGURACION, e);
        }

        return propiedades;
    }
}
