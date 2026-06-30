package com.ms_pagos.service;

import com.ms_pagos.dto.CrearPagoDTO;
import com.ms_pagos.dto.PagoDTO;
import com.ms_pagos.exception.*;
import com.ms_pagos.model.EstadoPago;
import com.ms_pagos.model.MetodoPago;
import com.ms_pagos.model.Pago;
import com.ms_pagos.repository.PagoRepository;
import com.ms_pagos.service.impl.PagoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {

    @Mock
    private PagoRepository pagoRepository;

    private PagoServiceImpl pagoService;

    private Pago pago;
    private CrearPagoDTO crearPagoDTO;

    @BeforeEach
    void setUp() {
        pagoService = new PagoServiceImpl(pagoRepository);

        pago = new Pago();
        pago.setIdTransaccion(1L);
        pago.setIdPedido(10L);
        pago.setMontoTotal(new BigDecimal("99.980"));
        pago.setMetodoPago(MetodoPago.TARJETA);
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setReferenciaExterna("REF-ABC123");
        pago.setCreadoEn(LocalDateTime.now());

        crearPagoDTO = new CrearPagoDTO();
        crearPagoDTO.setIdPedido(10L);
        crearPagoDTO.setMontoTotal(new BigDecimal("99.980"));
        crearPagoDTO.setMetodoPago(MetodoPago.TARJETA);
    }

    @Test
    void crearPago_CuandoValido_RetornaPagoDTO() {
        when(pagoRepository.findByIdPedido(10L)).thenReturn(Optional.empty());

        Pago guardado = new Pago();
        guardado.setIdTransaccion(1L);
        guardado.setIdPedido(10L);
        guardado.setMontoTotal(new BigDecimal("99.980"));
        guardado.setMetodoPago(MetodoPago.TARJETA);
        guardado.setEstado(EstadoPago.PENDIENTE);
        guardado.setReferenciaExterna("REF-ABC123");
        guardado.setCreadoEn(LocalDateTime.now());
        when(pagoRepository.save(any(Pago.class))).thenReturn(guardado);

        PagoDTO resultado = pagoService.crearPago(crearPagoDTO);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getIdPedido());
        assertEquals(new BigDecimal("99.980"), resultado.getMontoTotal());
        assertEquals(MetodoPago.TARJETA, resultado.getMetodoPago());
        assertEquals(EstadoPago.PENDIENTE, resultado.getEstado());
        assertNotNull(resultado.getReferenciaExterna());
    }

    @Test
    void crearPago_CuandoMontoCero_LanzaNoHayMontoPorPagarException() {
        crearPagoDTO.setMontoTotal(BigDecimal.ZERO);

        assertThrows(NoHayMontoPorPagarException.class, () -> pagoService.crearPago(crearPagoDTO));
    }

    @Test
    void crearPago_CuandoYaExistePagoParaPedido_LanzaIllegalStateException() {
        when(pagoRepository.findByIdPedido(10L)).thenReturn(Optional.of(pago));

        assertThrows(IllegalStateException.class, () -> pagoService.crearPago(crearPagoDTO));
    }

    @Test
    void procesarPago_CuandoValido_RetornaConEstadoCOMPLETADOoRECHAZADO() {
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> {
            Pago p = invocation.getArgument(0);
            p.setProcesadoEn(LocalDateTime.now());
            return p;
        });

        PagoDTO resultado = pagoService.procesarPago(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getIdTransaccion());
        assertTrue(resultado.getEstado() == EstadoPago.COMPLETADO || resultado.getEstado() == EstadoPago.RECHAZADO);
        assertNotNull(resultado.getProcesadoEn());
        verify(pagoRepository).save(any(Pago.class));
    }

    @Test
    void procesarPago_CuandoNoEstaPendiente_LanzaTransicionEstadoInvalidaException() {
        pago.setEstado(EstadoPago.COMPLETADO);
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(pago));

        assertThrows(TransicionEstadoInvalidaException.class, () -> pagoService.procesarPago(1L));
    }

    @Test
    void anularPago_CuandoPendiente_AnulaYRetorna() {
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(pago));

        Pago anulado = new Pago();
        anulado.setIdTransaccion(1L);
        anulado.setIdPedido(10L);
        anulado.setMontoTotal(new BigDecimal("99.980"));
        anulado.setMetodoPago(MetodoPago.TARJETA);
        anulado.setEstado(EstadoPago.ANULADO);
        anulado.setProcesadoEn(LocalDateTime.now());
        when(pagoRepository.save(any(Pago.class))).thenReturn(anulado);

        PagoDTO resultado = pagoService.anularPago(1L);

        assertEquals(EstadoPago.ANULADO, resultado.getEstado());
        assertNotNull(resultado.getProcesadoEn());
    }

    @Test
    void anularPago_CuandoNoEstaPendiente_LanzaTransicionEstadoInvalidaException() {
        pago.setEstado(EstadoPago.COMPLETADO);
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(pago));

        assertThrows(TransicionEstadoInvalidaException.class, () -> pagoService.anularPago(1L));
    }

    @Test
    void consultarPorId_CuandoExiste_RetornaPagoDTO() {
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(pago));

        PagoDTO resultado = pagoService.consultarPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getIdTransaccion());
        assertEquals(EstadoPago.PENDIENTE, resultado.getEstado());
    }

    @Test
    void consultarPorId_CuandoNoExiste_LanzaPagoNotFoundException() {
        when(pagoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(PagoNotFoundException.class, () -> pagoService.consultarPorId(999L));
    }

    @Test
    void consultarPorPedido_CuandoExiste_RetornaPagoDTO() {
        when(pagoRepository.findByIdPedido(10L)).thenReturn(Optional.of(pago));

        PagoDTO resultado = pagoService.consultarPorPedido(10L);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getIdPedido());
    }

    @Test
    void consultarPorPedido_CuandoNoExiste_LanzaPagoNotFoundException() {
        when(pagoRepository.findByIdPedido(999L)).thenReturn(Optional.empty());

        assertThrows(PagoNotFoundException.class, () -> pagoService.consultarPorPedido(999L));
    }

    @Test
    void listarTodos_RetornaLista() {
        when(pagoRepository.findAll()).thenReturn(List.of(pago));

        List<PagoDTO> resultado = pagoService.listarTodos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).getIdTransaccion());
    }

    @Test
    void reintentarPago_CuandoRechazado_ReintentaYRetorna() {
        pago.setEstado(EstadoPago.RECHAZADO);
        pago.setProcesadoEn(LocalDateTime.now());
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PagoDTO resultado = pagoService.reintentarPago(1L);

        assertNotNull(resultado);
        assertTrue(resultado.getEstado() == EstadoPago.COMPLETADO || resultado.getEstado() == EstadoPago.RECHAZADO);
        assertNotNull(resultado.getProcesadoEn());
        verify(pagoRepository, times(2)).save(any(Pago.class));
    }

    @Test
    void reintentarPago_CuandoNoEstaRechazado_LanzaTransicionEstadoInvalidaException() {
        pago.setEstado(EstadoPago.PENDIENTE);
        when(pagoRepository.findById(1L)).thenReturn(Optional.of(pago));

        assertThrows(TransicionEstadoInvalidaException.class, () -> pagoService.reintentarPago(1L));
    }
}
