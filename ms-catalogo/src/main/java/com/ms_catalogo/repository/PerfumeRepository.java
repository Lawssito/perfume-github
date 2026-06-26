package com.ms_catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.ms_catalogo.model.Perfume;

import java.util.List;

@Repository
public interface PerfumeRepository extends JpaRepository<Perfume, Long> {

    List<Perfume> findByNombreContainingIgnoreCase(String nombre);

    @Query("SELECT p FROM Perfume p WHERE " +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(p.marca.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(p.categoria.nombre) LIKE LOWER(CONCAT('%', :termino, '%'))")
    List<Perfume> buscarPorTermino(@Param("termino") String termino);

    List<Perfume> findByMarcaIdMarca(Long idMarca);

    List<Perfume> findByCategoriaIdCategoria(Long idCategoria);
}
