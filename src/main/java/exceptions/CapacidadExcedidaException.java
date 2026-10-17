package exceptions;

    public class CapacidadExcedidaException extends RuntimeException {
    public CapacidadExcedidaException() {
        super("La cantidad de comensales supera la capacidad máxima de la mesa ");
    }
}
