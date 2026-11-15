package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TelegramService {

    private static final String BOT_TOKEN = "9199856212:AAEOfCgI8x0V7v9l8oHtN8XrQCKV4AXtgrw";
    private static final String CHAT_ID = "9028284763";

    public static boolean enviarMensaje(String textoMensaje) {
        try {
            String url = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";

            // Estructura del JSON que exige Telegram
            String jsonBody = String.format("{\"chat_id\": \"%s\", \"text\": \"%s\"}", CHAT_ID, textoMensaje);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Si el código de estado es 200, el mensaje se envió con éxito
            return response.statusCode() == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}