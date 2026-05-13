package com.security_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.security_service.model.UsuarioRol;

@Repository
public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, Long> {
    // Buscar todos los roles asignados a un usuario específico
    List<UsuarioRol> findByIdUsuario(Long idUsuario);
}