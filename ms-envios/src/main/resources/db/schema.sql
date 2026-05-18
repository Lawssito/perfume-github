CREATE TABLE IF NOT EXISTS envios (
    id_envio BIGINT NOT NULL AUTO_INCREMENT,
    id_pedido BIGINT NOT NULL,
    direccion_destino VARCHAR(300) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    numero_tracking VARCHAR(100),
    courier VARCHAR(100),
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    entrega_estimada DATE,
    entregado_en DATETIME,
    PRIMARY KEY (id_envio),
    UNIQUE KEY uk_envios_pedido (id_pedido),
    UNIQUE KEY uk_envios_tracking (numero_tracking)
);
