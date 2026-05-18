CREATE TABLE IF NOT EXISTS transacciones (
    id_transaccion BIGINT NOT NULL AUTO_INCREMENT,
    id_pedido BIGINT NOT NULL,
    monto_total DECIMAL(10,2) NOT NULL,
    metodo_pago VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    referencia_externa VARCHAR(100),
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    procesado_en DATETIME,
    PRIMARY KEY (id_transaccion),
    UNIQUE KEY uk_transacciones_pedido (id_pedido),
    CONSTRAINT chk_monto_total CHECK (monto_total > 0)
);
