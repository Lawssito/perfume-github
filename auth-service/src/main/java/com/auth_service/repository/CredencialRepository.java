package com.auth_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.auth_service.model.Credencial;   
import java.util.Optional;

@Repository
public interface CredencialRepository extends JpaRepository<Credencial, Long> {
    Optional<Credencial> findByEmailLogin(String emailLogin);
}