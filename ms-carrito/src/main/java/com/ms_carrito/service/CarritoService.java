package com.ms_carrito.service;

import com.ms_carrito.dto.ActualizarCantidadDTO;
import com.ms_carrito.dto.AgregarItemDTO;
import com.ms_carrito.dto.CarritoDTO;

public interface CarritoService {
    CarritoDTO obtenerOCrearCarrito(Long idUsuario);
    CarritoDTO agregarItem(Long idUsuario, AgregarItemDTO dto);
    CarritoDTO actualizarCantidad(Long idUsuario, Long idItem, ActualizarCantidadDTO dto);
    CarritoDTO eliminarItem(Long idUsuario, Long idItem);
    void vaciarCarrito(Long idUsuario);
}