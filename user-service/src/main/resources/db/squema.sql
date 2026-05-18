CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(120) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    telefono VARCHAR(20),
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    PRIMARY KEY (id_usuario),
    UNIQUE KEY uk_usuarios_email (email)
);

CREATE TABLE IF NOT EXISTS direcciones (
    id_direccion BIGINT NOT NULL AUTO_INCREMENT,
    id_usuario BIGINT NOT NULL,
    calle VARCHAR(150) NOT NULL,
    numero VARCHAR(20),
    comuna VARCHAR(80) NOT NULL,
    ciudad VARCHAR(80) NOT NULL,
    tipo_alias VARCHAR(50),
    PRIMARY KEY (id_direccion),
    KEY idx_direcciones_usuario (id_usuario),
    CONSTRAINT fk_direccion_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);