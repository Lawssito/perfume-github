package com.ms_notificaciones.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ms_notificaciones.model.Notificacion;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    // Útil si necesitas consultar el historial de notificaciones de un usuario
    List<Notificacion> findByIdUsuarioOrderByFechaDesc(Long idUsuario);
}