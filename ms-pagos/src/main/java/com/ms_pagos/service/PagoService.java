package com.ms_pagos.service;

import com.ms_pagos.dto.CrearPagoDTO;
import com.ms_pagos.dto.PagoDTO;

import java.util.List;

public interface PagoService {
    PagoDTO crearPago(CrearPagoDTO dto);
    PagoDTO procesarPago(Long idTransaccion);
    PagoDTO anularPago(Long idTransaccion);
    PagoDTO consultarPorId(Long idTransaccion);
    PagoDTO consultarPorPedido(Long idPedido);
    List<PagoDTO> listarTodos();
    PagoDTO reintentarPago(Long idTransaccion);
}