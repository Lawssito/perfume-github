CREATE TABLE IF NOT EXISTS carritos (
    id_carrito BIGINT NOT NULL AUTO_INCREMENT,
    id_usuario BIGINT NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_carrito),
    UNIQUE KEY uk_carritos_usuario (id_usuario)
);

CREATE TABLE IF NOT EXISTS items_carrito (
    id_item BIGINT NOT NULL AUTO_INCREMENT,
    id_carrito BIGINT NOT NULL,
    id_variante BIGINT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id_item),
    UNIQUE KEY uq_carrito_variante (id_carrito, id_variante),
    KEY idx_items_carrito (id_carrito),
    CONSTRAINT fk_item_carrito FOREIGN KEY (id_carrito) REFERENCES carritos(id_carrito) ON DELETE CASCADE,
    CONSTRAINT chk_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_precio CHECK (precio_unitario > 0)
);
