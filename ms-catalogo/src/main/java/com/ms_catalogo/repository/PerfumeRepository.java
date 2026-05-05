package com.ms_catalogo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ms_catalogo.model.Genero;
import com.ms_catalogo.model.Perfume;

public interface PerfumeRepository extends JpaRepository<Perfume, Long>{
    List<Perfume> findByGenero(Genero genero);
    List<Perfume> findByMarcaContainingIgnoreCase(String marca);
}
