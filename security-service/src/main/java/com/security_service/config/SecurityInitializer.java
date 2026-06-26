package com.security_service.config;

import com.security_service.model.Rol;
import com.security_service.model.UsuarioRol;
import com.security_service.repository.RolRepository;
import com.security_service.repository.UsuarioRolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityInitializer implements ApplicationRunner {

    private final UsuarioRolRepository usuarioRolRepository;
    private final RolRepository rolRepository;

    @Value("${security.admin.default-user-id:0}")
    private Long defaultAdminUserId;

    @Override
    public void run(ApplicationArguments args) {
        if (defaultAdminUserId == null || defaultAdminUserId <= 0) {
            log.info("[INIT] security.admin.default-user-id no configurado — no se asigna ROLE_ADMIN automaticamente");
            return;
        }

        log.info("[INIT] Verificando roles para usuario ID={} (security.admin.default-user-id)", defaultAdminUserId);

        List<UsuarioRol> rolesActuales = usuarioRolRepository.findByIdUsuario(defaultAdminUserId);
        List<String> nombresRoles = rolesActuales.stream()
                .map(ur -> ur.getRol().getNombre())
                .collect(Collectors.toList());

        log.info("[INIT] Usuario {} tiene roles: {}", defaultAdminUserId, nombresRoles);

        if (!nombresRoles.contains("ROLE_CLIENTE")) {
            asignarRol(defaultAdminUserId, "ROLE_CLIENTE");
        }
        if (!nombresRoles.contains("ROLE_ADMIN")) {
            asignarRol(defaultAdminUserId, "ROLE_ADMIN");
        }

        log.info("[INIT] Roles asegurados para usuario ID={}: ROLE_CLIENTE, ROLE_ADMIN", defaultAdminUserId);
    }

    private void asignarRol(Long idUsuario, String nombreRol) {
        Optional<Rol> rolOpt = rolRepository.findByNombre(nombreRol);
        if (rolOpt.isEmpty()) {
            log.warn("[INIT] Rol {} no existe en BD — saltando asignacion", nombreRol);
            return;
        }
        usuarioRolRepository.save(UsuarioRol.builder()
                .idUsuario(idUsuario)
                .rol(rolOpt.get())
                .build());
        log.info("[INIT] Rol {} asignado a usuario ID={}", nombreRol, idUsuario);
    }
}
