package com.restaurante.repository;

import com.restaurante.entity.HistorialEstadoMesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialEstadoMesaRepository extends JpaRepository<HistorialEstadoMesa, Long> {

    List<HistorialEstadoMesa> findByMesaIdOrderByFechaCambioDesc(Long mesaId);
}
