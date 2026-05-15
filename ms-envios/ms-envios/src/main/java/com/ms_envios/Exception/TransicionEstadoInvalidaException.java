package com.ms_envios.Exception;

import com.ms_envios.model.EstadoEnvio;

public class TransicionEstadoInvalidaException extends RuntimeException{
    public TransicionEstadoInvalidaException(EstadoEnvio actual, EstadoEnvio solicitado) {
        super("Transicion invalida: no se puede pasar de " + actual + " a " + solicitado);
    }
}
