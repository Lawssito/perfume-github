package com.ms_pagos.exception;

public class PagoNotFoundException extends RuntimeException {
    public PagoNotFoundException(Long id) {
        super("No existe pago con ID: " + id);
    }
}
