package service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class TelegramServiceTest {

    private Properties configuracion;

    @BeforeEach
    void prepararConfiguracion() throws Exception {
        assumeTrue(System.getenv("TELEGRAM_BOT_TOKEN") == null
                        && System.getenv("TELEGRAM_CHAT_ID") == null,
                "las variables de entorno tienen prioridad sobre el archivo de configuracion");

        Field campo = TelegramService.class.getDeclaredField("CONFIGURACION");
        campo.setAccessible(true);
        configuracion = (Properties) campo.get(null);
        configuracion.clear();
    }

    @AfterEach
    void limpiarConfiguracion() {
        if (configuracion != null) {
            configuracion.clear();
        }
    }

    @Test
    @DisplayName("sin credenciales el servicio no esta configurado")
    void sinCredencialesNoEstaConfigurado() {
        boolean configurado = TelegramService.estaConfigurado();

        assertFalse(configurado);
    }

    @Test
    @DisplayName("con token y chat en el archivo el servicio esta configurado")
    void conTokenYChatEstaConfigurado() {
        configuracion.setProperty("telegram.token", "token-de-prueba");
        configuracion.setProperty("telegram.chat", "123456");

        boolean configurado = TelegramService.estaConfigurado();

        assertTrue(configurado);
    }

    @Test
    @DisplayName("un token en blanco cuenta como no configurado")
    void conTokenEnBlancoNoEstaConfigurado() {
        configuracion.setProperty("telegram.token", "   ");
        configuracion.setProperty("telegram.chat", "123456");

        boolean configurado = TelegramService.estaConfigurado();

        assertFalse(configurado);
    }

    @Test
    @DisplayName("un chat en blanco cuenta como no configurado")
    void conChatEnBlancoNoEstaConfigurado() {
        configuracion.setProperty("telegram.token", "token-de-prueba");
        configuracion.setProperty("telegram.chat", "   ");

        boolean configurado = TelegramService.estaConfigurado();

        assertFalse(configurado);
    }

    @Test
    @DisplayName("sin credenciales no se intenta el envio y se devuelve false")
    void sinCredencialesNoIntentaEnviar() throws Exception {
        CompletableFuture<Boolean> envio = TelegramService.enviarMensajeAsync("Pago recibido");

        boolean resultado = envio.get(5, TimeUnit.SECONDS);

        assertTrue(envio.isDone());
        assertFalse(resultado);
    }

    @Test
    @DisplayName("con un token invalido Telegram no acepta el mensaje y se devuelve false")
    void conCredencialesInvalidasDevuelveFalse() throws Exception {
        configuracion.setProperty("telegram.token", "token-invalido");
        configuracion.setProperty("telegram.chat", "123456");

        boolean resultado = TelegramService.enviarMensajeAsync("Pago recibido").get(15, TimeUnit.SECONDS);

        assertFalse(resultado);
    }
}
