package com.ms_catalogo.service;

import com.ms_catalogo.dto.*;
import com.ms_catalogo.model.*;
import com.ms_catalogo.repository.*;
import com.ms_catalogo.service.impl.CatalogoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceImplTest {

    @Mock
    private PerfumeRepository perfumeRepository;
    @Mock
    private MarcaRepository marcaRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private VarianteRepository varianteRepository;

    private CatalogoServiceImpl catalogoService;

    private Marca marca;
    private Categoria categoria;
    private Perfume perfume;
    private Variante variante;

    @BeforeEach
    void setUp() {
        catalogoService = new CatalogoServiceImpl(perfumeRepository, marcaRepository, categoriaRepository, varianteRepository);

        marca = Marca.builder().idMarca(1L).nombre("Marca Test").build();
        categoria = Categoria.builder().idCategoria(1L).nombre("Categoria Test").descripcion("Desc").build();
        variante = Variante.builder()
                .idVariante(1L)
                .sku("SKU001")
                .ml(100)
                .precio(new BigDecimal("49.990"))
                .build();
        perfume = Perfume.builder()
                .idPerfume(1L)
                .nombre("Perfume Test")
                .concentracion("EDT")
                .descripcion("Descripcion")
                .estado("ACTIVO")
                .marca(marca)
                .categoria(categoria)
                .variantes(List.of(variante))
                .build();
        variante.setPerfume(perfume);
    }

    @Test
    void listarCatalogoCompleto_CuandoHayPerfumes_RetornaLista() {
        when(perfumeRepository.findAll()).thenReturn(List.of(perfume));

        List<PerfumeResponseDTO> resultado = catalogoService.listarCatalogoCompleto();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Perfume Test", resultado.get(0).getNombre());
    }

    @Test
    void obtenerPerfume_CuandoExiste_RetornaPerfume() {
        when(perfumeRepository.findById(1L)).thenReturn(Optional.of(perfume));

        PerfumeResponseDTO resultado = catalogoService.obtenerPerfume(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getIdPerfume());
        assertEquals("Perfume Test", resultado.getNombre());
    }

    @Test
    void obtenerPerfume_CuandoNoExiste_LanzaIllegalArgumentException() {
        when(perfumeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> catalogoService.obtenerPerfume(999L));
    }

    @Test
    void crearPerfume_CuandoMarcaYCategoriaExisten_CreaYRetornaPerfume() {
        PerfumeDTO dto = new PerfumeDTO();
        dto.setIdMarca(1L);
        dto.setIdCategoria(1L);
        dto.setNombre("Nuevo Perfume");
        dto.setConcentracion("EDP");
        dto.setDescripcion("Descripcion");

        when(marcaRepository.findById(1L)).thenReturn(Optional.of(marca));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(perfumeRepository.save(any(Perfume.class))).thenReturn(perfume);

        PerfumeResponseDTO resultado = catalogoService.crearPerfume(dto);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getIdPerfume());
        assertEquals("ACTIVO", resultado.getEstado());
    }

    @Test
    void crearPerfume_CuandoMarcaNoExiste_LanzaIllegalArgumentException() {
        PerfumeDTO dto = new PerfumeDTO();
        dto.setIdMarca(999L);
        dto.setIdCategoria(1L);

        when(marcaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> catalogoService.crearPerfume(dto));
    }

    @Test
    void crearPerfume_CuandoCategoriaNoExiste_LanzaIllegalArgumentException() {
        PerfumeDTO dto = new PerfumeDTO();
        dto.setIdMarca(1L);
        dto.setIdCategoria(999L);

        when(marcaRepository.findById(1L)).thenReturn(Optional.of(marca));
        when(categoriaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> catalogoService.crearPerfume(dto));
    }

    @Test
    void actualizarPerfume_CuandoExiste_ActualizaYRetorna() {
        PerfumeDTO dto = new PerfumeDTO();
        dto.setIdMarca(1L);
        dto.setIdCategoria(1L);
        dto.setNombre("Actualizado");
        dto.setConcentracion("EDP");
        dto.setDescripcion("Nueva desc");

        Marca marca2 = Marca.builder().idMarca(1L).nombre("Marca Test").build();
        Categoria categoria2 = Categoria.builder().idCategoria(1L).nombre("Categoria Test").descripcion("Desc").build();
        Perfume perfumeActualizado = Perfume.builder()
                .idPerfume(1L).nombre("Actualizado").concentracion("EDP")
                .descripcion("Nueva desc").estado("ACTIVO")
                .marca(marca2).categoria(categoria2)
                .variantes(List.of())
                .build();

        when(perfumeRepository.findById(1L)).thenReturn(Optional.of(perfume));
        when(marcaRepository.findById(1L)).thenReturn(Optional.of(marca));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(perfumeRepository.save(any(Perfume.class))).thenReturn(perfumeActualizado);

        PerfumeResponseDTO resultado = catalogoService.actualizarPerfume(1L, dto);

        assertNotNull(resultado);
        assertEquals("Actualizado", resultado.getNombre());
    }

    @Test
    void actualizarPerfume_CuandoNoExiste_LanzaIllegalArgumentException() {
        when(perfumeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> catalogoService.actualizarPerfume(999L, new PerfumeDTO()));
    }

    @Test
    void eliminarPerfume_CuandoExiste_CambiaEstadoADescontinuado() {
        when(perfumeRepository.findById(1L)).thenReturn(Optional.of(perfume));
        when(perfumeRepository.save(any(Perfume.class))).thenReturn(perfume);

        catalogoService.eliminarPerfume(1L);

        assertEquals("DESCONTINUADO", perfume.getEstado());
        verify(perfumeRepository).save(perfume);
    }

    @Test
    void eliminarPerfume_CuandoNoExiste_LanzaIllegalArgumentException() {
        when(perfumeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> catalogoService.eliminarPerfume(999L));
    }

    @Test
    void obtenerVariante_CuandoExiste_RetornaVariante() {
        when(varianteRepository.findById(1L)).thenReturn(Optional.of(variante));

        VarianteResponseDTO resultado = catalogoService.obtenerVariante(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getIdVariante());
        assertEquals("SKU001", resultado.getSku());
    }

    @Test
    void obtenerVariante_CuandoNoExiste_LanzaIllegalArgumentException() {
        when(varianteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> catalogoService.obtenerVariante(999L));
    }

    @Test
    void agregarVariante_CuandoPerfumeExiste_CreaYRetorna() {
        VarianteDTO dto = new VarianteDTO();
        dto.setSku("SKU002");
        dto.setMl(50);
        dto.setPrecio(new BigDecimal("29.990"));

        Variante nuevaVariante = Variante.builder()
                .idVariante(2L).sku("SKU002").ml(50)
                .precio(new BigDecimal("29.990")).perfume(perfume)
                .build();

        when(perfumeRepository.findById(1L)).thenReturn(Optional.of(perfume));
        when(varianteRepository.save(any(Variante.class))).thenReturn(nuevaVariante);

        VarianteResponseDTO resultado = catalogoService.agregarVariante(1L, dto);

        assertNotNull(resultado);
        assertEquals(2L, resultado.getIdVariante());
        assertEquals("SKU002", resultado.getSku());
    }

    @Test
    void actualizarVariante_CuandoExiste_ActualizaYRetorna() {
        VarianteDTO dto = new VarianteDTO();
        dto.setSku("SKU001-UPD");
        dto.setMl(150);
        dto.setPrecio(new BigDecimal("59.990"));

        when(varianteRepository.findById(1L)).thenReturn(Optional.of(variante));
        when(varianteRepository.save(any(Variante.class))).thenReturn(variante);

        VarianteResponseDTO resultado = catalogoService.actualizarVariante(1L, dto);

        assertNotNull(resultado);
        assertEquals("SKU001-UPD", resultado.getSku());
        assertEquals(150, resultado.getMl());
    }

    @Test
    void actualizarVariante_CuandoNoExiste_LanzaIllegalArgumentException() {
        when(varianteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> catalogoService.actualizarVariante(999L, new VarianteDTO()));
    }

    @Test
    void eliminarVariante_CuandoExiste_EliminaFisicamente() {
        when(varianteRepository.findById(1L)).thenReturn(Optional.of(variante));

        catalogoService.eliminarVariante(1L);

        verify(varianteRepository).delete(variante);
    }

    @Test
    void eliminarVariante_CuandoNoExiste_LanzaIllegalArgumentException() {
        when(varianteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> catalogoService.eliminarVariante(999L));
    }

    @Test
    void listarMarcas_CuandoExisten_RetornaLista() {
        when(marcaRepository.findAll()).thenReturn(List.of(marca));

        List<MarcaResponseDTO> resultado = catalogoService.listarMarcas();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Marca Test", resultado.get(0).getNombre());
    }

    @Test
    void crearMarca_CuandoValida_CreaYRetorna() {
        MarcaDTO dto = new MarcaDTO();
        dto.setNombre("Nueva Marca");

        Marca nuevaMarca = Marca.builder().idMarca(2L).nombre("Nueva Marca").build();
        when(marcaRepository.save(any(Marca.class))).thenReturn(nuevaMarca);

        MarcaResponseDTO resultado = catalogoService.crearMarca(dto);

        assertNotNull(resultado);
        assertEquals(2L, resultado.getIdMarca());
        assertEquals("Nueva Marca", resultado.getNombre());
    }

    @Test
    void listarCategorias_CuandoExisten_RetornaLista() {
        when(categoriaRepository.findAll()).thenReturn(List.of(categoria));

        List<CategoriaResponseDTO> resultado = catalogoService.listarCategorias();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Categoria Test", resultado.get(0).getNombre());
    }

    @Test
    void crearCategoria_CuandoValida_CreaYRetorna() {
        CategoriaDTO dto = new CategoriaDTO();
        dto.setNombre("Nueva Categoria");
        dto.setDescripcion("Desc");

        Categoria nuevaCategoria = Categoria.builder().idCategoria(2L).nombre("Nueva Categoria").descripcion("Desc").build();
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(nuevaCategoria);

        CategoriaResponseDTO resultado = catalogoService.crearCategoria(dto);

        assertNotNull(resultado);
        assertEquals(2L, resultado.getIdCategoria());
        assertEquals("Nueva Categoria", resultado.getNombre());
    }

    @Test
    void buscarPerfumes_CuandoCoinciden_RetornaLista() {
        when(perfumeRepository.buscarPorTermino("Test")).thenReturn(List.of(perfume));

        List<PerfumeResponseDTO> resultado = catalogoService.buscarPerfumes("Test");

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }

    @Test
    void listarPorMarca_CuandoExisten_RetornaLista() {
        when(perfumeRepository.findByMarcaIdMarca(1L)).thenReturn(List.of(perfume));

        List<PerfumeResponseDTO> resultado = catalogoService.listarPorMarca(1L);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).getIdMarca());
    }

    @Test
    void listarPorNombre_CuandoCoinciden_RetornaLista() {
        when(perfumeRepository.findByNombreContainingIgnoreCase("Perfume")).thenReturn(List.of(perfume));

        List<PerfumeResponseDTO> resultado = catalogoService.listarPorNombre("Perfume");

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
    }
}
