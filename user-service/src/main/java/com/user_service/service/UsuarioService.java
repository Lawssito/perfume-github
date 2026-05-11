package com.user_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.user_service.dto.direccionDTO;
import com.user_service.dto.usuarioDTO;
import com.user_service.model.Direccion;
import com.user_service.model.Usuario;
import com.user_service.repository.UsuarioRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Transactional
    public Usuario crearUsuario(usuarioDTO dto) {
        log.info("Iniciando lógica de negocio para crear usuario con email: {}", dto.getEmail());
        
        // Validación de negocio (ej: email no repetido)
        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
            log.error("El email {} ya se encuentra registrado", dto.getEmail());
            throw new IllegalArgumentException("Email ya registrado");
        }

        Usuario usuario = Usuario.builder()
                .email(dto.getEmail())
                .nombre(dto.getNombre())
                .telefono(dto.getTelefono())
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        log.info("Usuario creado en base de datos exitosamente con ID: {}", guardado.getIdUsuario());
        
        return guardado;
    }

    @Transactional
    public Direccion agregarDireccion(Long idUsuario, direccionDTO dto) {
        log.info("Iniciando proceso para agregar dirección al usuario ID: {}", idUsuario);
        
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> {
                    log.error("Usuario con ID {} no encontrado", idUsuario);
                    return new RuntimeException("Usuario no encontrado");
                });

        Direccion direccion = Direccion.builder()
                .usuario(usuario)
                .calle(dto.getCalle())
                .numero(dto.getNumero())
                .comuna(dto.getComuna())
                .ciudad(dto.getCiudad())
                .tipoAlias(dto.getTipoAlias())
                .build();

        usuario.getDirecciones().add(direccion);
        usuarioRepository.save(usuario); // Guarda en cascada
        
        log.info("Dirección agregada exitosamente al usuario ID: {}", idUsuario);
        return direccion;
    }
}