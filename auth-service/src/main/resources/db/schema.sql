CREATE TABLE IF NOT EXISTS credenciales (
    id_credencial BIGINT NOT NULL AUTO_INCREMENT,
    id_usuario BIGINT NOT NULL,
    email VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    estado_cuenta VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_credencial),
    UNIQUE KEY uk_credenciales_usuario (id_usuario),
    UNIQUE KEY uk_credenciales_email (email)
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id_refresh_tk BIGINT NOT NULL AUTO_INCREMENT,
    id_credencial BIGINT NOT NULL,
    token_hash VARCHAR(500) NOT NULL,
    expira_en DATETIME NOT NULL,
    revocado BIT NOT NULL DEFAULT 0,
    PRIMARY KEY (id_refresh_tk),
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    KEY idx_refresh_credencial (id_credencial),
    CONSTRAINT fk_refresh_credencial FOREIGN KEY (id_credencial) REFERENCES credenciales(id_credencial)
);
