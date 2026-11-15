package support;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;

import static org.junit.jupiter.api.Assertions.*;

public final class ContratoFxml {


    private ContratoFxml() {}

    public static void verificar(Class<?> controlador, String vista) throws Exception {
        var fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        try (var contenido = controlador.getResourceAsStream("/views/" + vista)) {
            assertNotNull(contenido, vista);
            var documento = fabrica.newDocumentBuilder().parse(contenido);
            var raiz = documento.getDocumentElement();
            String fx = raiz.lookupNamespaceURI("fx");
            assertNotNull(fx);
            assertEquals(controlador.getName(), raiz.getAttributeNS(fx, "controller"));
            var identificadores = new java.util.HashSet<String>();
            var elementos = documento.getElementsByTagName("*");
            for (int i = 0; i < elementos.getLength(); i++) {
                var elemento = (Element) elementos.item(i);
                String id = elemento.getAttributeNS(fx, "id");
                if (!id.isEmpty()) {
                    assertTrue(identificadores.add(id), "fx:id duplicado: " + id);
                }
                String accion = elemento.getAttribute("onAction");
                if (accion.startsWith("#")) {
                    String metodo = accion.substring(1);
                    assertTrue(Arrays.stream(controlador.getDeclaredMethods())
                            .anyMatch(m -> m.getName().equals(metodo)
                                    && (m.isAnnotationPresent(FXML.class) || Modifier.isPublic(m.getModifiers()))
                                    && (m.getParameterCount() == 0 || (m.getParameterCount() == 1
                                        && m.getParameterTypes()[0].isAssignableFrom(ActionEvent.class)))), accion);
                }
            }
            for (var campo : controlador.getDeclaredFields()) {
                if (campo.isAnnotationPresent(FXML.class)) {
                    assertTrue(identificadores.contains(campo.getName()), "Falta fx:id para " + campo.getName());
                }
            }
        }
    }
}





