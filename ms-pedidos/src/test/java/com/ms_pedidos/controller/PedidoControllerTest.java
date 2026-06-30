package com.ms_pedidos.controller;

import com.ms_pedidos.dto.*;
import com.ms_pedidos.exception.GlobalExceptionHandler;
import com.ms_pedidos.model.EstadoPedido;
import com.ms_pedidos.service.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PedidoService pedidoService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PedidoController(pedidoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private PedidoDTO crearPedidoDTO() {
        DetallePedidoDTO detalle = new DetallePedidoDTO(1L, 1L, "Perfume Test", 100, 2, new BigDecimal("49.990"), new BigDecimal("99.980"));
        List<DetallePedidoDTO> detalles = new ArrayList<>();
        detalles.add(detalle);
        return new PedidoDTO(1L, 1L, EstadoPedido.CREADO, new BigDecimal("99.980"),
                LocalDateTime.now(), "Direccion Test", "CHILEXPRESS", detalles);
    }

    @Test
    void crearPedido_ConUsuarioAutenticado_DebeRetornar201() throws Exception {
        when(pedidoService.crearPedido(any(CrearPedidoDTO.class), eq(1L))).thenReturn(crearPedidoDTO());

        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direccionEntrega\":\"Direccion Test\",\"courier\":\"CHILEXPRESS\"}")
                        .header("X-User-Id", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPedido").value(1));
    }

    @Test
    void listarMisPedidos_ConUsuarioAutenticado_DebeRetornar200() throws Exception {
        when(pedidoService.listarPorUsuario(1L)).thenReturn(List.of(crearPedidoDTO()));

        mockMvc.perform(get("/api/pedidos/mis-pedidos")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idPedido").value(1));
    }

    @Test
    void listarTodos_SinRolAdmin_DebeRetornar403() throws Exception {
        mockMvc.perform(get("/api/pedidos")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarTodos_ConRolAdmin_DebeRetornar200() throws Exception {
        when(pedidoService.listarTodos()).thenReturn(List.of(crearPedidoDTO()));

        mockMvc.perform(get("/api/pedidos")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idPedido").value(1));
    }

    @Test
    void consultarPorId_CuandoEsMismoUsuario_DebeRetornar200() throws Exception {
        PedidoDTO pedido = crearPedidoDTO();
        when(pedidoService.consultarPorId(1L)).thenReturn(pedido);

        mockMvc.perform(get("/api/pedidos/1")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPedido").value(1));
    }

    @Test
    void consultarPorId_CuandoEsOtroUsuario_DebeRetornar403() throws Exception {
        PedidoDTO pedido = crearPedidoDTO();
        when(pedidoService.consultarPorId(1L)).thenReturn(pedido);

        mockMvc.perform(get("/api/pedidos/1")
                        .header("X-User-Id", "2"))
                .andExpect(status().isForbidden());
    }

    @Test
    void pagarPedido_CuandoEsMismoUsuario_DebeRetornar200() throws Exception {
        PedidoDTO pedidoCreado = crearPedidoDTO();
        PedidoDTO pedidoPagado = crearPedidoDTO();
        pedidoPagado.setEstado(EstadoPedido.PAGADO);

        when(pedidoService.consultarPorId(1L)).thenReturn(pedidoCreado);
        when(pedidoService.pagarPedido(1L)).thenReturn(pedidoPagado);

        mockMvc.perform(post("/api/pedidos/1/pagar")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADO"));
    }

    @Test
    void actualizarEstado_ConRolAdmin_DebeRetornar200() throws Exception {
        PedidoDTO pedidoActualizado = crearPedidoDTO();
        pedidoActualizado.setEstado(EstadoPedido.ENVIADO);
        when(pedidoService.actualizarEstado(eq(1L), any(EstadoPedido.class))).thenReturn(pedidoActualizado);

        mockMvc.perform(put("/api/pedidos/1/estado")
                        .param("estado", "ENVIADO")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ENVIADO"));
    }

    @Test
    void actualizarEstado_SinRolAdmin_DebeRetornar403() throws Exception {
        mockMvc.perform(put("/api/pedidos/1/estado")
                        .param("estado", "ENVIADO")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearPedido_ConDatosInvalidos_DebeRetornar400() throws Exception {
        mockMvc.perform(post("/api/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direccionEntrega\":\"\"}")
                        .header("X-User-Id", "1"))
                .andExpect(status().isBadRequest());
    }
}
