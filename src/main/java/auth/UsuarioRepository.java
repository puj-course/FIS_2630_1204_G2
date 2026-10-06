package com.restaurante.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCodigoEmpleadoAndActivoTrue(String codigoEmpleado);
}
