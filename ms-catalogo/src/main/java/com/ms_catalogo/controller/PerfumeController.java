package com.ms_catalogo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ms_catalogo.dto.PerfumeDto;
import com.ms_catalogo.service.PerfumeService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/perfumes")
public class PerfumeController {

    @Autowired
    private PerfumeService service;

    @GetMapping
    public ResponseEntity<List<PerfumeDto>> listarPerfumes() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PerfumeDto> obtenerPerfume(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<PerfumeDto> crearPerfume(@RequestBody PerfumeDto perfumeDTO) {
        PerfumeDto nuevoPerfume = service.crearPerfume(perfumeDTO);
        return new ResponseEntity<>(nuevoPerfume, HttpStatus.CREATED);
    }
}