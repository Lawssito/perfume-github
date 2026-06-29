package com.ms_envios.service;

import com.ms_envios.dto.AvanzarEstadoDTO;
import com.ms_envios.dto.CrearEnvioDTO;
import com.ms_envios.dto.EnvioDTO;
import com.ms_envios.exception.EnvioNotFoundException;
import com.ms_envios.exception.TransicionEstadoInvalidaException;
import com.ms_envios.model.Envio;
import com.ms_envios.model.EstadoEnvio;
import com.ms_envios.repository.EnvioRepository;
import com.ms_envios.service.impl.EnvioServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnvioServiceImplTest {

    @Mock
    private EnvioRepository envioRepository;

    @InjectMocks
    private EnvioServiceImpl envioService;

    private Envio crearEnvioBase() {
        Envio e = new Envio();
        e.setIdEnvio(1L);
        e.setIdPedido(100L);
        e.setDireccionDestino("Av. Providencia 123");
        e.setEstado(EstadoEnvio.PENDIENTE);
        e.setCourier("Starken");
        e.setNumeroTracking("TRK-ABC12345");
        e.setFechaCreacion(LocalDateTime.now());
        e.setEntregaEstimada(LocalDate.now().plusDays(5));
        return e;
    }

    @Test
    void crearEnvio_exitoso() {
        CrearEnvioDTO dto = new CrearEnvioDTO();
        dto.setIdPedido(100L);
        dto.setDireccionDestino("Av. Providencia 123");
        dto.setCourier("Starken");

        Envio envioGuardado = crearEnvioBase();

        when(envioRepository.findByIdPedido(100L)).thenReturn(Optional.empty());
        when(envioRepository.save(any(Envio.class))).thenReturn(envioGuardado);

        EnvioDTO result = envioService.crearEnvio(dto);

        assertEquals(1L, result.getIdEnvio());
        assertEquals(100L, result.getIdPedido());
        assertEquals(EstadoEnvio.PENDIENTE, result.getEstado());
        verify(envioRepository).save(any(Envio.class));
    }

    @Test
    void crearEnvio_duplicado_lanzaIllegalStateException() {
        CrearEnvioDTO dto = new CrearEnvioDTO();
        dto.setIdPedido(100L);
        dto.setDireccionDestino("Av. Providencia 123");
        dto.setCourier("Starken");

        when(envioRepository.findByIdPedido(100L)).thenReturn(Optional.of(new Envio()));

        assertThrows(IllegalStateException.class, () -> envioService.crearEnvio(dto));
        verify(envioRepository, never()).save(any());
    }

    @Test
    void avanzarEstado_PENDIENTE_A_EN_PREPARACION() {
        Envio envio = crearEnvioBase();
        AvanzarEstadoDTO dto = new AvanzarEstadoDTO();
        dto.setEstado(EstadoEnvio.EN_PREPARACION);

        when(envioRepository.findById(1L)).thenReturn(Optional.of(envio));
        when(envioRepository.save(any(Envio.class))).thenAnswer(i -> i.getArgument(0));

        EnvioDTO result = envioService.avanzarEstado(1L, dto);

        assertEquals(EstadoEnvio.EN_PREPARACION, result.getEstado());
    }

    @Test
    void avanzarEstado_EN_CAMINO_A_ENTREGADO() {
        Envio envio = crearEnvioBase();
        envio.setEstado(EstadoEnvio.EN_CAMINO);
        AvanzarEstadoDTO dto = new AvanzarEstadoDTO();
        dto.setEstado(EstadoEnvio.ENTREGADO);

        when(envioRepository.findById(1L)).thenReturn(Optional.of(envio));
        when(envioRepository.save(any(Envio.class))).thenAnswer(i -> i.getArgument(0));

        EnvioDTO result = envioService.avanzarEstado(1L, dto);

        assertEquals(EstadoEnvio.ENTREGADO, result.getEstado());
        assertNotNull(result.getEntregadoEn());
    }

    @Test
    void avanzarEstado_ENTREGADO_A_CANCELADO_lanzaTransicionInvalida() {
        Envio envio = crearEnvioBase();
        envio.setEstado(EstadoEnvio.ENTREGADO);
        AvanzarEstadoDTO dto = new AvanzarEstadoDTO();
        dto.setEstado(EstadoEnvio.CANCELADO);

        when(envioRepository.findById(1L)).thenReturn(Optional.of(envio));

        assertThrows(TransicionEstadoInvalidaException.class,
                () -> envioService.avanzarEstado(1L, dto));
        verify(envioRepository, never()).save(any());
    }

    @Test
    void cancelarEnvio_PENDIENTE_A_CANCELADO() {
        Envio envio = crearEnvioBase();
        AvanzarEstadoDTO dto = new AvanzarEstadoDTO();
        dto.setEstado(EstadoEnvio.CANCELADO);

        when(envioRepository.findById(1L)).thenReturn(Optional.of(envio));
        when(envioRepository.save(any(Envio.class))).thenAnswer(i -> i.getArgument(0));

        EnvioDTO result = envioService.cancelarEnvio(1L);

        assertEquals(EstadoEnvio.CANCELADO, result.getEstado());
    }

    @Test
    void consultarPorId_encontrado() {
        Envio envio = crearEnvioBase();
        when(envioRepository.findById(1L)).thenReturn(Optional.of(envio));

        EnvioDTO result = envioService.consultarPorId(1L);

        assertEquals(1L, result.getIdEnvio());
        assertEquals(100L, result.getIdPedido());
    }

    @Test
    void consultarPorId_noEncontrado_lanzaExcepcion() {
        when(envioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EnvioNotFoundException.class, () -> envioService.consultarPorId(99L));
    }

    @Test
    void consultarPorPedido_encontrado() {
        Envio envio = crearEnvioBase();
        when(envioRepository.findByIdPedido(100L)).thenReturn(Optional.of(envio));

        EnvioDTO result = envioService.consultarPorPedido(100L);

        assertEquals(100L, result.getIdPedido());
    }

    @Test
    void consultarPorPedido_noEncontrado_lanzaExcepcion() {
        when(envioRepository.findByIdPedido(99L)).thenReturn(Optional.empty());

        assertThrows(EnvioNotFoundException.class, () -> envioService.consultarPorPedido(99L));
    }

    @Test
    void listarTodos() {
        Envio e1 = crearEnvioBase();
        Envio e2 = crearEnvioBase();
        e2.setIdEnvio(2L);
        e2.setIdPedido(200L);

        when(envioRepository.findAll()).thenReturn(List.of(e1, e2));

        List<EnvioDTO> result = envioService.listarTodos();

        assertEquals(2, result.size());
    }

    @Test
    void listarPorEstado() {
        Envio e1 = crearEnvioBase();
        Envio e2 = crearEnvioBase();
        e2.setIdEnvio(2L);

        when(envioRepository.findByEstado(EstadoEnvio.PENDIENTE)).thenReturn(List.of(e1, e2));

        List<EnvioDTO> result = envioService.listarPorEstado(EstadoEnvio.PENDIENTE);

        assertEquals(2, result.size());
    }
}
