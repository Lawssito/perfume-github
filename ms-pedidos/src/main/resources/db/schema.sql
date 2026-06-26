CREATE TABLE IF NOT EXISTS pedidos (
    id_pedido BIGINT NOT NULL AUTO_INCREMENT,
    id_usuario BIGINT NOT NULL,
    estado VARCHAR(30) NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    direccion_entrega VARCHAR(300) DEFAULT NULL,
    courier VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (id_pedido),
    KEY idx_pedidos_usuario (id_usuario),
    CONSTRAINT chk_pedido_total CHECK (total >= 0)
);

CREATE TABLE IF NOT EXISTS detalle_pedidos (
    id_detalle BIGINT NOT NULL AUTO_INCREMENT,
    id_pedido BIGINT NOT NULL,
    id_variante BIGINT NOT NULL,
    nombre_producto VARCHAR(150) NOT NULL,
    ml INT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id_detalle),
    KEY idx_detalles_pedido (id_pedido),
    CONSTRAINT fk_detalle_pedido FOREIGN KEY (id_pedido) REFERENCES pedidos(id_pedido) ON DELETE CASCADE,
    CONSTRAINT chk_detalle_ml CHECK (ml > 0),
    CONSTRAINT chk_detalle_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_detalle_precio CHECK (precio_unitario > 0)
);
