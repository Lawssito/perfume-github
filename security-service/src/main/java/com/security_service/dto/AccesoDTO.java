package com.security_service.dto;

import com.security_service.model.MetodoHttp;

public record AccesoDTO(
    String nombrePermiso,
    String ruta,
    MetodoHttp metodo
){}
