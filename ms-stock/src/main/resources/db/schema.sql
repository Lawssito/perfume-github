CREATE TABLE IF NOT EXISTS inventario (
    id_inventario BIGINT NOT NULL AUTO_INCREMENT,
    id_variante BIGINT NOT NULL,
    cantidad_disponible INT NOT NULL DEFAULT 0,
    cantidad_reservada INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id_inventario),
    UNIQUE KEY uk_inventario_variante (id_variante),
    CONSTRAINT chk_disponible CHECK (cantidad_disponible >= 0),
    CONSTRAINT chk_reservada CHECK (cantidad_reservada >= 0)
);

CREATE TABLE IF NOT EXISTS idempotencia_keys (
    id BIGINT NOT NULL AUTO_INCREMENT,
    idempotency_key VARCHAR(100) NOT NULL,
    id_variante BIGINT NOT NULL,
    operacion VARCHAR(50) NOT NULL,
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotency_key (idempotency_key)
);
