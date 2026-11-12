
package exceptions;

public class MesaConPedidoActivoException extends RuntimeException {


    /** Requerido por Serializable, que RuntimeException implementa. */
    private static final long serialVersionUID = 7078278594027370242L;

    public MesaConPedidoActivoException(String mensaje) {
        super(mensaje);
    }
}
