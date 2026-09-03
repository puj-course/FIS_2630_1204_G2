package com.restaurante.repository;

import com.restaurante.entity.Zona;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZonaRepository extends JpaRepository<Zona, Long> {
}