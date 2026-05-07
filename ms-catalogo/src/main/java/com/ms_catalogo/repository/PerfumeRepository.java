package com.ms_catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.ms_catalogo.model.Genero;
import com.ms_catalogo.model.Perfume;
import java.util.List;

@Repository
public interface PerfumeRepository extends JpaRepository<Perfume, Long> {
    List<Perfume> findByMarcaContainingIgnoreCase(String marca);
    List<Perfume> findByGenero(Genero genero);
}