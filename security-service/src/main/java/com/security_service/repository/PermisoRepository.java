package com.security_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.security_service.model.Permiso;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {
    Optional<Permiso> findByNombre(String nombre);
}
