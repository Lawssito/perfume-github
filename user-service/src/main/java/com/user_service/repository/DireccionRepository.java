package com.user_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.user_service.model.Direccion;

@Repository
public interface DireccionRepository extends JpaRepository<Direccion, Long> {

    List<Direccion> findByUsuarioIdUsuario(Long idUsuario);

    Optional<Direccion> findByIdDireccionAndUsuarioIdUsuario(Long idDireccion, Long idUsuario);
}
