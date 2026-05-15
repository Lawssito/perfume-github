package com.ms_envios.dto;

import com.ms_envios.model.EstadoEnvio;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnvioDTO {
    private Long          idEnvio;
    private Long          idPedido;
    private String        direccionDestino;
    private EstadoEnvio   estado;
    private String        numeroTracking;
    private String        courier;
    private LocalDateTime fechaCreacion;
    private LocalDate     entregaEstimada;
    private LocalDateTime entregadoEn;
}
