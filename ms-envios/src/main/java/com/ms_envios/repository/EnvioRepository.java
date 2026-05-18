package com.ms_envios.repository;

import com.ms_envios.model.Envio;
import com.ms_envios.model.EstadoEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnvioRepository extends JpaRepository<Envio, Long> {

    Optional<Envio> findByIdPedido(Long idPedido);

    List<Envio> findByEstado(EstadoEnvio estado);
}
