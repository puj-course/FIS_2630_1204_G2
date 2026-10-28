package service;

import entity.ListaEspera;
import enums.EstadoListaEspera;
import repository.ListaEsperaRepository;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class ListaEsperaService {

    private final ListaEsperaRepository listaEsperaRepository = new ListaEsperaRepository();
    private final MesaService mesaService = new MesaService(); // Para interactuar con las mesas

    /**
     * Registra un grupo de clientes en la lista de espera.
     */
    public ListaEspera registrarEnEspera(String nombreCliente, int cantidadPersonas) throws SQLException {
        if (cantidadPersonas <= 0) {
            throw new IllegalArgumentException("La cantidad de personas debe ser mayor a 0.");
        }

        ListaEspera item = new ListaEspera();
        item.setNombreCliente(nombreCliente);
        item.setCantidadPersonas(cantidadPersonas);
        item.setHoraIngreso(LocalDateTime.now());
        item.setEstado(EstadoListaEspera.EN_ESPERA); // Uso del enum base

        return listaEsperaRepository.save(item);
    }

    /**
     * Retorna la lista de espera activa filtrada por el enum EN_ESPERA.
     */
    public List<ListaEspera> obtenerListaEsperaActiva() throws SQLException {
        return listaEsperaRepository.findByEstadoOrderByHoraIngresoAsc(EstadoListaEspera.EN_ESPERA);
    }
    /**
     * Asigna una mesa disponible a un grupo en espera validando capacidad y usando enums.
     */
    public void asignarMesa(long listaEsperaId, long mesaId, int capacidadMesa) throws SQLException, Exception {
        ListaEspera grupo = listaEsperaRepository.findById(listaEsperaId)
                .orElseThrow(() -> new Exception("El registro de espera no fue encontrado."));

        // Validación basada en Enum
        if (grupo.getEstado() != EstadoListaEspera.EN_ESPERA) {
            throw new Exception("El grupo ya no se encuentra activo en la lista de espera.");
        }

        // Criterio de aceptación: La mesa asignada debe tener capacidad suficiente
        if (capacidadMesa < grupo.getCantidadPersonas()) {
            throw new Exception("La mesa seleccionada no tiene capacidad suficiente para este grupo (" + grupo.getCantidadPersonas() + " personas).");
        }

        // 1. Cambiar estado de la mesa a OCUPADA (o el código correspondiente que maneje tu MesaService)
        mesaService.cambiarEstado(mesaId, "OCUPADA", "ASIGNACION_LISTA_ESPERA");

        // 2. Marcar al grupo como ASIGNADO usando el enum
        grupo.setEstado(EstadoListaEspera.ASIGNADO);
        listaEsperaRepository.update(grupo);
    }

    /**
     * Cancela o elimina un registro de la lista de espera pasándolo a CANCELADO.
     */
    public void cancelarRegistro(long listaEsperaId) throws SQLException, Exception {
        ListaEspera grupo = listaEsperaRepository.findById(listaEsperaId)
                .orElseThrow(() -> new Exception("El registro de espera no fue encontrado."));

        grupo.setEstado(EstadoListaEspera.CANCELADO); // Uso del enum
        listaEsperaRepository.update(grupo);
    }
}