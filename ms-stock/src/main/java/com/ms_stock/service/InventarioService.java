package com.ms_stock.service;

import com.ms_stock.dto.ConfirmarReservaDTO;
import com.ms_stock.dto.InventarioDTO;
import com.ms_stock.dto.LiberarReservaDTO;
import com.ms_stock.dto.ReducirStockDTO;
import com.ms_stock.dto.ReponerStockDTO;
import com.ms_stock.dto.ReservarStockDTO;
import java.util.List;

public interface InventarioService {
    InventarioDTO consultarPorVariante(Long idVariante);
    List<InventarioDTO> listarTodo();
    InventarioDTO crearInventario(Long idVariante);
    InventarioDTO reponerStock(Long idVariante, ReponerStockDTO dto);
    InventarioDTO reducirStock(Long idVariante, ReducirStockDTO dto);
    InventarioDTO reservarStock(Long idVariante, ReservarStockDTO dto);
    InventarioDTO confirmarReserva(Long idVariante, ConfirmarReservaDTO dto);
    InventarioDTO liberarReserva(Long idVariante, LiberarReservaDTO dto);
    void eliminarInventario(Long idVariante);
}
