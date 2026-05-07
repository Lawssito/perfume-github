package com.security_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.security_service.model.Estado;
import com.security_service.model.RolPermiso;

public interface RolPermisoRepository extends JpaRepository<RolPermiso, Long>{

    List<RolPermiso> findByRolIdAndEstado(Long rolId, Estado estado);
}
