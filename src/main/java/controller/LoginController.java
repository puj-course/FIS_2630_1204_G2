package controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import service.AutenticacionService;
import service.AutenticacionService.Resultado;

import java.util.function.Consumer;

/**
 * Pantalla de inicio de sesión.
 *
 * Solo se ocupa de la interacción: recoge lo escrito, se lo pasa al servicio de
 * autenticación y muestra el resultado. La verificación de credenciales y la
 * apertura de la sesión están en {@link AutenticacionService}.
 */
public class LoginController {

    @FXML private TextField campoUsuario;
    @FXML private PasswordField campoContrasena;
    @FXML private Button botonEntrar;
    @FXML private HBox cajaError;
    @FXML private Label mensajeError;

    private final AutenticacionService autenticacion = new AutenticacionService();

    /** Lo llama Main para saber que ya puede mostrar la navegación. */
    private Consumer<Void> alEntrar = v -> { };

    public void setAlEntrar(Runnable accion) {
        this.alEntrar = v -> accion.run();
    }

    @FXML
    private void initialize() {
        ocultarError();

        // El botón se habilita solo cuando hay algo en los dos campos, para que
        // no se pueda enviar el formulario vacío.
        botonEntrar.disableProperty().bind(
                campoUsuario.textProperty().isEmpty()
                        .or(campoContrasena.textProperty().isEmpty()));

        // Al corregir lo escrito, el error deja de estorbar.
        campoUsuario.textProperty().addListener((obs, antes, ahora) -> ocultarError());
        campoContrasena.textProperty().addListener((obs, antes, ahora) -> ocultarError());

        Platform.runLater(() -> campoUsuario.requestFocus());
    }

    @FXML
    private void entrar() {
        String usuario = campoUsuario.getText();
        String contrasena = campoContrasena.getText();

        // No se toca botonEntrar.setDisable(): su estado esta ligado al contenido
        // de los campos, y una propiedad ligada no se puede fijar a mano.
        Resultado resultado = autenticacion.iniciarSesion(usuario, contrasena);

        if (resultado == Resultado.OK) {
            ocultarError();
            campoContrasena.clear();
            alEntrar.accept(null);
            return;
        }

        // Se limpia antes de mostrar el mensaje: al vaciar el campo se dispara el
        // listener que oculta el error, y si el orden fuera el contrario el
        // mensaje se borraria justo despues de aparecer.
        campoContrasena.clear();
        mostrarError(AutenticacionService.mensajeDe(resultado));
        campoContrasena.requestFocus();
    }

    // ------------------------------------------------------------------

    private void mostrarError(String texto) {
        mensajeError.setText(texto);
        cajaError.setVisible(true);
        cajaError.setManaged(true);
        marcarCampos(true);
    }

    private void ocultarError() {
        cajaError.setVisible(false);
        cajaError.setManaged(false);
        mensajeError.setText("");
        marcarCampos(false);
    }

    private void marcarCampos(boolean conError) {
        for (Node campo : new Node[]{campoUsuario, campoContrasena}) {
            campo.getStyleClass().remove("con-error");
            if (conError) {
                campo.getStyleClass().add("con-error");
            }
        }
    }
}
