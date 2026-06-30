package com.ms_notificaciones.service.impl;

import com.ms_notificaciones.dto.EventoNotificacionDTO;
import com.ms_notificaciones.dto.NotificacionResponseDTO;
import com.ms_notificaciones.model.Notificacion;
import com.ms_notificaciones.repository.NotificacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceImplTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private SimuladorEmail simuladorEmail;

    @InjectMocks
    private NotificacionServiceImpl notificacionService;

    private Notificacion crearNotificacionBase() {
        return Notificacion.builder()
                .idNotificacion(1L)
                .idUsuario(1L)
                .mensaje("Su pedido #123 ha sido confirmado")
                .estadoEnvio("PENDIENTE")
                .fecha(LocalDateTime.now())
                .build();
    }

    @Test
    void procesarEventoYEnviar_exitoso_estadoEnviado() {
        EventoNotificacionDTO evento = new EventoNotificacionDTO();
        evento.setIdUsuario(1L);
        evento.setTipoEvento("PEDIDO_CREADO");
        evento.setMensaje("Su pedido #123 ha sido confirmado");

        Notificacion notifPendiente = Notificacion.builder()
                .idNotificacion(1L)
                .idUsuario(1L)
                .mensaje("Su pedido #123 ha sido confirmado")
                .estadoEnvio("PENDIENTE")
                .fecha(LocalDateTime.now())
                .build();

        Notificacion notifEnviada = Notificacion.builder()
                .idNotificacion(1L)
                .idUsuario(1L)
                .mensaje("Su pedido #123 ha sido confirmado")
                .estadoEnvio("ENVIADO")
                .fecha(LocalDateTime.now())
                .build();

        when(notificacionRepository.save(any(Notificacion.class)))
                .thenReturn(notifPendiente)
                .thenReturn(notifEnviada);

        NotificacionResponseDTO result = notificacionService.procesarEventoYEnviar(evento);

        assertEquals("ENVIADO", result.getEstadoEnvio());
        assertEquals(1L, result.getIdUsuario());
        assertEquals("Su pedido #123 ha sido confirmado", result.getMensaje());
    }

    @Test
    void procesarEventoYEnviar_falloSimulado_estadoFallido() throws Exception {
        EventoNotificacionDTO evento = new EventoNotificacionDTO();
        evento.setIdUsuario(1L);
        evento.setTipoEvento("PEDIDO_CREADO");
        evento.setMensaje("Su pedido #123 ha sido confirmado");

        Notificacion notifPendiente = Notificacion.builder()
                .idNotificacion(1L)
                .idUsuario(1L)
                .mensaje("Su pedido #123 ha sido confirmado")
                .estadoEnvio("PENDIENTE")
                .fecha(LocalDateTime.now())
                .build();

        Notificacion notifFallida = Notificacion.builder()
                .idNotificacion(1L)
                .idUsuario(1L)
                .mensaje("Su pedido #123 ha sido confirmado")
                .estadoEnvio("FALLIDO")
                .fecha(LocalDateTime.now())
                .build();

        doThrow(new InterruptedException("SMTP timeout")).when(simuladorEmail).enviar(any());
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenReturn(notifPendiente)
                .thenReturn(notifFallida);

        NotificacionResponseDTO result = notificacionService.procesarEventoYEnviar(evento);

        assertEquals("FALLIDO", result.getEstadoEnvio());
    }

    @Test
    void listarTodas() {
        Notificacion n1 = crearNotificacionBase();
        Notificacion n2 = Notificacion.builder()
                .idNotificacion(2L)
                .idUsuario(2L)
                .mensaje("Otro mensaje")
                .estadoEnvio("ENVIADO")
                .fecha(LocalDateTime.now())
                .build();

        when(notificacionRepository.findAll()).thenReturn(List.of(n1, n2));

        List<NotificacionResponseDTO> result = notificacionService.listarTodas();

        assertEquals(2, result.size());
    }

    @Test
    void obtenerPorId_encontrado() {
        Notificacion notif = crearNotificacionBase();

        when(notificacionRepository.findById(1L)).thenReturn(Optional.of(notif));

        NotificacionResponseDTO result = notificacionService.obtenerPorId(1L);

        assertEquals(1L, result.getIdNotificacion());
        assertEquals(1L, result.getIdUsuario());
    }

    @Test
    void obtenerPorId_noEncontrado_lanzaIAE() {
        when(notificacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> notificacionService.obtenerPorId(99L));
    }

    @Test
    void listarPorUsuario() {
        Notificacion n1 = crearNotificacionBase();
        Notificacion n2 = Notificacion.builder()
                .idNotificacion(2L)
                .idUsuario(1L)
                .mensaje("Segunda notificacion")
                .estadoEnvio("ENVIADO")
                .fecha(LocalDateTime.now())
                .build();

        when(notificacionRepository.findByIdUsuarioOrderByFechaDesc(1L))
                .thenReturn(List.of(n2, n1));

        List<NotificacionResponseDTO> result = notificacionService.listarPorUsuario(1L);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(n -> n.getIdUsuario().equals(1L)));
    }

    @Test
    void eliminar_exitoso() {
        Notificacion notif = crearNotificacionBase();

        when(notificacionRepository.findById(1L)).thenReturn(Optional.of(notif));

        notificacionService.eliminar(1L);

        verify(notificacionRepository).delete(notif);
    }

    @Test
    void eliminar_noEncontrado_lanzaIAE() {
        when(notificacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> notificacionService.eliminar(99L));
        verify(notificacionRepository, never()).delete(any());
    }
}
