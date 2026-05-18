CREATE TABLE IF NOT EXISTS inventario (
    id_inventario BIGINT NOT NULL AUTO_INCREMENT,
    id_variante BIGINT NOT NULL,
    cantidad_disponible INT NOT NULL DEFAULT 0,
    cantidad_reservada INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id_inventario),
    UNIQUE KEY uk_inventario_variante (id_variante),
    CONSTRAINT chk_disponible CHECK (cantidad_disponible >= 0),
    CONSTRAINT chk_reservada CHECK (cantidad_reservada >= 0)
);
