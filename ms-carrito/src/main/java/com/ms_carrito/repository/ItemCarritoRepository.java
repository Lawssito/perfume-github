package com.ms_carrito.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.ms_carrito.model.ItemCarrito;

@Repository
public interface ItemCarritoRepository extends JpaRepository<ItemCarrito, Long>{
    Optional<ItemCarrito> findByCarrito_IdCarritoAndIdVariante(Long idCarrito, Long idVariante);
}
