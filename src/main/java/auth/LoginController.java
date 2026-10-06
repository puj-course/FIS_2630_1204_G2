package com.restaurante.auth;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final UsuarioRepository usuarioRepository;

    public LoginController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session) {
        if (request == null || request.codigo() == null || request.password() == null
                || request.codigo().isBlank() || request.password().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Código y contraseña son obligatorios"));
        }

        Usuario usuario = usuarioRepository.findByCodigoEmpleadoAndActivoTrue(request.codigo().trim())
                .orElse(null);

        if (usuario == null || !PasswordUtil.verify(request.password(), usuario.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Credenciales incorrectas"));
        }

        if (!"CAJERO".equalsIgnoreCase(usuario.getRol())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("mensaje", "El usuario no tiene rol CAJERO"));
        }

        session.setAttribute("usuarioId", usuario.getId());
        session.setAttribute("codigoEmpleado", usuario.getCodigoEmpleado());
        session.setAttribute("rol", usuario.getRol());
        session.setAttribute("nombre", (usuario.getNombre() + " " + (usuario.getApellido() == null ? "" : usuario.getApellido())).trim());

        return ResponseEntity.ok(Map.of(
                "mensaje", "Inicio de sesión correcto",
                "nombre", session.getAttribute("nombre"),
                "rol", usuario.getRol()
        ));
    }

    @GetMapping("/sesion")
    public ResponseEntity<?> sesion(HttpSession session) {
        if (session.getAttribute("usuarioId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("autenticado", false));
        }
        return ResponseEntity.ok(Map.of(
                "autenticado", true,
                "nombre", session.getAttribute("nombre"),
                "rol", session.getAttribute("rol")
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada"));
    }

    public record LoginRequest(String codigo, String password) {}
}
