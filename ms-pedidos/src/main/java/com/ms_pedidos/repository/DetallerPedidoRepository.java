package com.ms_pedidos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.ms_pedidos.model.DetallePedido;

@Repository
public interface DetallerPedidoRepository extends JpaRepository<DetallePedido, Long>{
    List<DetallePedido> findByPedido_IdPedido(Long idPedido);
}
