package service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class TelegramService {
    private static final String BOT_TOKEN = "9199856212:AAEOfCgI8x0V7v9l8oHtN8XrQCKV4AXtgrw";
    private static final String CHAT_ID = "9028284763";

    public static CompletableFuture<Boolean> enviarMensajeAsync(String mensaje) {
        String urlString = String.format("https://api.telegram.org/bot%s/sendMessage", BOT_TOKEN);

        // Codificar el texto correctamente para la URL
        String encodedMessage = URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
        String payload = String.format("chat_id=%s&text=%s", CHAT_ID, encodedMessage);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> response.statusCode() == 200)
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return false;
                });
    }
}
