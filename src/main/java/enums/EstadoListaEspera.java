package enums;

public enum EstadoListaEspera {
    EN_ESPERA,   // El grupo está esperando una mesa libre
    ASIGNADO,    // Ya se le asignó una mesa y salió de la lista activa
    CANCELADO    // El grupo canceló o abandonó la lista
}
