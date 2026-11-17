package service;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de TelegramService.
 *
 * No se envía ningún mensaje de verdad. Lo que se comprueba es el camino que
 * importa para la aplicación: que cuando no hay credenciales configuradas el
 * servicio avise y responda que no, en vez de intentar la petición, lanzar una
 * excepción o dejar la interfaz esperando.
 *
 * En el pipeline no existe conf/telegram.properties, porque está excluido del
 * repositorio, así que estas pruebas corren siempre allí. En la máquina de
 * alguien que sí lo tenga configurado se omiten, para no mandarle un mensaje
 * al chat cada vez que ejecuta las pruebas.
 */
class TelegramServiceTest {

    @Test
    @DisplayName("Sin credenciales, el servicio se reporta como no configurado")
    void sinCredencialesNoEstaConfigurado() {
        Assumptions.assumeFalse(TelegramService.estaConfigurado(),
                "Hay credenciales de Telegram en este equipo: se omite la prueba.");

        assertFalse(TelegramService.estaConfigurado(),
                "sin token ni chat, estaConfigurado() debe devolver false");
    }

    @Test
    @DisplayName("Sin credenciales, enviar devuelve false sin lanzar excepción")
    void sinCredencialesElEnvioDevuelveFalse() throws Exception {
        Assumptions.assumeFalse(TelegramService.estaConfigurado(),
                "Hay credenciales de Telegram en este equipo: se omite la prueba.");

        Boolean enviado = TelegramService
                .enviarMensajeAsync("mensaje de prueba")
                .get(5, TimeUnit.SECONDS);

        assertNotNull(enviado, "la promesa no debe resolverse en null");
        assertFalse(enviado, "sin credenciales no se puede haber enviado nada");
    }

    @Test
    @DisplayName("Sin credenciales, la promesa ya viene resuelta y no se queda esperando")
    void sinCredencialesLaPromesaNoSeQuedaEsperando() {
        Assumptions.assumeFalse(TelegramService.estaConfigurado(),
                "Hay credenciales de Telegram en este equipo: se omite la prueba.");

        assertTrue(TelegramService.enviarMensajeAsync("otro mensaje").isDone(),
                "si no hay credenciales no se abre ninguna conexion, "
                        + "asi que la respuesta tiene que estar lista de inmediato");
    }

    @Test
    @DisplayName("Un mensaje vacío se trata igual que cualquier otro")
    void mensajeVacioNoRompe() {
        Assumptions.assumeFalse(TelegramService.estaConfigurado(),
                "Hay credenciales de Telegram en este equipo: se omite la prueba.");

        assertFalse(TelegramService.enviarMensajeAsync("").join(),
                "un mensaje vacio no debe lanzar excepcion, solo no enviarse");
    }
}
