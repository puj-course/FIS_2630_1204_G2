package util;

import service.TelegramService;

public class TestTelegram {
    public static void main(String[] args) {
        System.out.println("Iniciando prueba de envío a Telegram...");

        String mensajePrueba = "Hola, este es un mensaje de prueba enviado directamente desde la lógica de Java.";

        // Llamamos al servicio directamente
        TelegramService.enviarMensajeAsync(mensajePrueba)
                .thenAccept(exito -> {
                    if (exito) {
                        System.out.println("¡Éxito! El mensaje se envió correctamente a Telegram.");
                    } else {
                        System.err.println("Fallo: No se pudo enviar el mensaje. Revisa tu Token o Chat ID.");
                    }
                })
                .join(); // .join() es vital aquí para evitar que el programa principal cierre el hilo antes de que la respuesta HTTP llegue

        System.out.println("Prueba finalizada.");
    }
}
