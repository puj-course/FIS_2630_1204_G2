package com.restaurante.repository;

import com.restaurante.entity.AsignacionMesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AsignacionMesaRepository extends JpaRepository<AsignacionMesa, Long> {

    Optional<AsignacionMesa> findByMesaIdAndIsActiveTrue(Long mesaId);

    List<AsignacionMesa> findByUsuarioIdAndIsActiveTrue(Long usuarioId);
}
