package com.auth_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.auth_service.model.Credencial;

public interface CredencialRepository extends JpaRepository<Credencial, Long>{
    Optional<Credencial> findfindByUsrname(String username);
}
