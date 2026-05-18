CREATE TABLE IF NOT EXISTS notificaciones (
    id_notificacion BIGINT NOT NULL AUTO_INCREMENT,
    id_usuario BIGINT NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado_envio VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    PRIMARY KEY (id_notificacion),
    KEY idx_notificaciones_usuario (id_usuario)
);
