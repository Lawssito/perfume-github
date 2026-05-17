package com.user_service.service;

import com.user_service.dto.direccionDTO;
import com.user_service.dto.usuarioDTO;
import com.user_service.model.Direccion;
import com.user_service.model.Usuario;

public interface IUsuarioService {
    Usuario crearUsuario(usuarioDTO dto);
    
    Direccion agregarDireccion(Long idUsuario, direccionDTO dto);
}

