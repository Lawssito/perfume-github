package com.ms_pagos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ms_pagos.model.Pago;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByIdPedido(Long idPedido);

    List<Pago> findByEstado(com.ms_pagos.model.EstadoPago estado);

}
