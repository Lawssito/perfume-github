package com.ms_catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.ms_catalogo.model.Perfume;

@Repository
public interface PerfumeRepository extends JpaRepository<Perfume, Long>{}
