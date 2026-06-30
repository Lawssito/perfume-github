package com.ms_catalogo.controller;

import com.ms_catalogo.dto.*;
import com.ms_catalogo.exception.GlobalExceptionHandler;
import com.ms_catalogo.service.CatalogoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CatalogoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CatalogoService catalogoService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CatalogoController(catalogoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private PerfumeResponseDTO crearPerfumeDTO() {
        VarianteResponseDTO v = new VarianteResponseDTO(1L, 1L, "SKU001", 100, new BigDecimal("49.990"));
        return new PerfumeResponseDTO(1L, "Perfume Test", "EDT", "Descripcion", "ACTIVO",
                1L, "Marca Test", 1L, "Categoria Test", List.of(v));
    }

    @Test
    void obtenerCatalogo_DebeRetornar200() throws Exception {
        when(catalogoService.listarCatalogoCompleto()).thenReturn(List.of(crearPerfumeDTO()));

        mockMvc.perform(get("/api/catalogo/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idPerfume").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Perfume Test"));
    }

    @Test
    void obtenerPerfume_CuandoExiste_DebeRetornar200() throws Exception {
        when(catalogoService.obtenerPerfume(1L)).thenReturn(crearPerfumeDTO());

        mockMvc.perform(get("/api/catalogo/perfumes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPerfume").value(1));
    }

    @Test
    void obtenerPerfume_CuandoNoExiste_DebeRetornar404() throws Exception {
        when(catalogoService.obtenerPerfume(999L)).thenThrow(new RuntimeException("Perfume no encontrado"));

        mockMvc.perform(get("/api/catalogo/perfumes/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void obtenerVariante_CuandoExiste_DebeRetornar200() throws Exception {
        VarianteResponseDTO v = new VarianteResponseDTO(1L, 1L, "SKU001", 100, new BigDecimal("49.990"));
        when(catalogoService.obtenerVariante(1L)).thenReturn(v);

        mockMvc.perform(get("/api/catalogo/variantes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idVariante").value(1));
    }

    @Test
    void crearPerfume_SinRolAdmin_DebeRetornar403() throws Exception {
        String json = """
                {"nombre":"Nuevo","concentracion":"EDT","descripcion":"Test","idMarca":1,"idCategoria":1}
                """;

        mockMvc.perform(post("/api/catalogo/perfumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearPerfume_ConRolAdmin_DebeRetornar201() throws Exception {
        when(catalogoService.crearPerfume(any(PerfumeDTO.class))).thenReturn(crearPerfumeDTO());

        String json = """
                {"nombre":"Nuevo","concentracion":"EDT","descripcion":"Test","idMarca":1,"idCategoria":1}
                """;

        mockMvc.perform(post("/api/catalogo/perfumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPerfume").value(1));
    }

    @Test
    void crearPerfume_SinHeaderRol_DebeRetornar201_PorSerFeign() throws Exception {
        when(catalogoService.crearPerfume(any(PerfumeDTO.class))).thenReturn(crearPerfumeDTO());

        String json = """
                {"nombre":"Nuevo","concentracion":"EDT","descripcion":"Test","idMarca":1,"idCategoria":1}
                """;

        mockMvc.perform(post("/api/catalogo/perfumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void eliminarPerfume_ConRolAdmin_DebeRetornar204() throws Exception {
        doNothing().when(catalogoService).eliminarPerfume(1L);

        mockMvc.perform(delete("/api/catalogo/perfumes/1")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminarPerfume_SinRolAdmin_DebeRetornar403() throws Exception {
        mockMvc.perform(delete("/api/catalogo/perfumes/1")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarMarcas_DebeRetornar200() throws Exception {
        MarcaResponseDTO m = new MarcaResponseDTO(1L, "Marca Test");
        when(catalogoService.listarMarcas()).thenReturn(List.of(m));

        mockMvc.perform(get("/api/catalogo/marcas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Marca Test"));
    }

    @Test
    void listarCategorias_DebeRetornar200() throws Exception {
        CategoriaResponseDTO c = new CategoriaResponseDTO(1L, "Categoria Test", "Desc");
        when(catalogoService.listarCategorias()).thenReturn(List.of(c));

        mockMvc.perform(get("/api/catalogo/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Categoria Test"));
    }

    @Test
    void buscarPerfumes_DebeRetornar200() throws Exception {
        when(catalogoService.buscarPerfumes("Aqua")).thenReturn(List.of(crearPerfumeDTO()));

        mockMvc.perform(get("/api/catalogo/buscar/Aqua"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Perfume Test"));
    }

    @Test
    void crearMarca_ConRolAdmin_DebeRetornar201() throws Exception {
        MarcaResponseDTO m = new MarcaResponseDTO(1L, "Nueva Marca");
        when(catalogoService.crearMarca(any(MarcaDTO.class))).thenReturn(m);

        mockMvc.perform(post("/api/catalogo/marcas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Nueva Marca\"}")
                        .header("X-User-Roles", "ROLE_ADMIN"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idMarca").value(1));
    }

    @Test
    void listarPorMarca_DebeRetornar200() throws Exception {
        when(catalogoService.listarPorMarca(1L)).thenReturn(List.of(crearPerfumeDTO()));

        mockMvc.perform(get("/api/catalogo/productos/marca/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idMarca").value(1));
    }

    @Test
    void actualizarPerfume_SinRolAdmin_DebeRetornar403() throws Exception {
        mockMvc.perform(put("/api/catalogo/perfumes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Edit\",\"concentracion\":\"EDP\",\"idMarca\":1,\"idCategoria\":1}")
                        .header("X-User-Roles", "ROLE_USER"))
                .andExpect(status().isForbidden());
    }
}
