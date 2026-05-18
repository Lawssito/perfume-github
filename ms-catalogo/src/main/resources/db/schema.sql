CREATE TABLE IF NOT EXISTS marcas (
    id_marca BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    PRIMARY KEY (id_marca),
    UNIQUE KEY uk_marcas_nombre (nombre)
);

CREATE TABLE IF NOT EXISTS categorias (
    id_categoria BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    PRIMARY KEY (id_categoria),
    UNIQUE KEY uk_categorias_nombre (nombre)
);

CREATE TABLE IF NOT EXISTS perfumes (
    id_perfume BIGINT NOT NULL AUTO_INCREMENT,
    id_categoria BIGINT NOT NULL,
    id_marca BIGINT NOT NULL,
    concentracion VARCHAR(30) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    PRIMARY KEY (id_perfume),
    KEY idx_perfumes_categoria (id_categoria),
    KEY idx_perfumes_marca (id_marca),
    CONSTRAINT fk_perfume_categoria FOREIGN KEY (id_categoria) REFERENCES categorias(id_categoria),
    CONSTRAINT fk_perfume_marca FOREIGN KEY (id_marca) REFERENCES marcas(id_marca)
);

CREATE TABLE IF NOT EXISTS variantes (
    id_variante BIGINT NOT NULL AUTO_INCREMENT,
    id_perfume BIGINT NOT NULL,
    sku VARCHAR(50) NOT NULL,
    ml INT NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id_variante),
    UNIQUE KEY uk_variantes_sku (sku),
    KEY idx_variantes_perfume (id_perfume),
    CONSTRAINT fk_variante_perfume FOREIGN KEY (id_perfume) REFERENCES perfumes(id_perfume),
    CONSTRAINT chk_variante_ml CHECK (ml > 0),
    CONSTRAINT chk_variante_precio CHECK (precio > 0)
);

INSERT IGNORE INTO marcas (id_marca, nombre) VALUES (1, 'Chanel'), (2, 'Dior');
INSERT IGNORE INTO categorias (id_categoria, nombre, descripcion) VALUES
    (1, 'Masculino', 'Fragancias para hombre'),
    (2, 'Femenino', 'Fragancias para mujer');