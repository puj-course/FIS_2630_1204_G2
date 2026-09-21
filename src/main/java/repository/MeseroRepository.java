package com.restaurante.repository;

import com.restaurante.entity.Mesero;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeseroRepository extends JpaRepository<Mesero, Long> {
}