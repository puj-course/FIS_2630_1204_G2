package exceptions;

public class ReservaNotFoundException extends RuntimeException {


    /** Requerido por Serializable, que RuntimeException implementa. */
    private static final long serialVersionUID = 4820380518252931246L;

    public ReservaNotFoundException(String mensaje) {
        super(mensaje);
    }
}
