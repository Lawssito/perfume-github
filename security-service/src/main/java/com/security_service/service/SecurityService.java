package com.security_service.service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.security_service.dto.AccesoDTO;
import com.security_service.model.Estado;
import com.security_service.model.Rol;
import com.security_service.model.RolPermiso;
import com.security_service.repository.RolPermisoRepository;
import com.security_service.repository.RolRepository;

@Service
public class SecurityService {
@Autowired
    private RolRepository rolRepository;

    @Autowired
    private RolPermisoRepository rolPermisoRepository;

    public List<AccesoDTO> obtenerPermisosPorRol(String nombreRol) {
        // 1. Buscamos el rol por su nombre (Ej: "ADMIN")
        Rol rol = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + nombreRol));

        // 2. Buscamos las relaciones Activas en la tabla intermedia
        List<RolPermiso> relaciones = rolPermisoRepository.findByRolIdAndEstado(rol.getId(), Estado.ACTIVO);

        // 3. Extraemos los permisos de esa relación y los convertimos en DTOs
        return relaciones.stream()
                .filter(rp -> rp.getPermiso().getEstado() == Estado.ACTIVO) // Validamos que el permiso esté activo
                .map(rp -> new AccesoDTO(
                        rp.getPermiso().getNombre(),
                        rp.getPermiso().getRuta(),
                        rp.getPermiso().getMetodo()
                ))
                .collect(Collectors.toList());
    }
}
