package com.ms_catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.ms_catalogo.model.Variante;

@Repository
public interface VarianteRepository extends JpaRepository<Variante, Long>{

}
