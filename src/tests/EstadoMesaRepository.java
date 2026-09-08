package com.restaurante.repository;

import com.restaurante.entity.EstadoMesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoMesaRepository extends JpaRepository<EstadoMesa, Long> {

    Optional<EstadoMesa> findByCodigoEstado(String codigoEstado);
}
