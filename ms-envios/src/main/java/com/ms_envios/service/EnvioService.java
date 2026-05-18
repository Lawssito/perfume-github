package com.ms_envios.service;

import com.ms_envios.dto.AvanzarEstadoDTO;
import com.ms_envios.dto.CrearEnvioDTO;
import com.ms_envios.dto.EnvioDTO;
import com.ms_envios.model.EstadoEnvio;

import java.util.List;

public interface EnvioService {
    EnvioDTO crearEnvio(CrearEnvioDTO dto);
    EnvioDTO avanzarEstado(Long idEnvio, AvanzarEstadoDTO dto);
    EnvioDTO cancelarEnvio(Long idEnvio);
    EnvioDTO consultarPorId(Long idEnvio);
    EnvioDTO consultarPorPedido(Long idPedido);
    List<EnvioDTO> listarTodos();
    List<EnvioDTO> listarPorEstado(EstadoEnvio estado);
}