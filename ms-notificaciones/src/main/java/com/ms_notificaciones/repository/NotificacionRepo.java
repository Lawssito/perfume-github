package com.ms_notificaciones.repository;

import com.proyecto.msnotificaciones.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepo extends JpaRepository<Notificacion, Long> {
    // Útil si necesitas consultar el historial de notificaciones de un usuario
    List<Notificacion> findByIdUsuarioOrderByFechaDesc(Long idUsuario);
}