CREATE TABLE IF NOT EXISTS permisos (
    id_permiso BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(80) NOT NULL,
    PRIMARY KEY (id_permiso),
    UNIQUE KEY uk_permisos_nombre (nombre)
);

CREATE TABLE IF NOT EXISTS roles (
    id_rol BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL,
    PRIMARY KEY (id_rol),
    UNIQUE KEY uk_roles_nombre (nombre)
);

CREATE TABLE IF NOT EXISTS rol_permisos (
    id_rol BIGINT NOT NULL,
    id_permiso BIGINT NOT NULL,
    PRIMARY KEY (id_rol, id_permiso),
    CONSTRAINT fk_rp_rol FOREIGN KEY (id_rol) REFERENCES roles(id_rol),
    CONSTRAINT fk_rp_permiso FOREIGN KEY (id_permiso) REFERENCES permisos(id_permiso)
);

CREATE TABLE IF NOT EXISTS usuario_roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    id_usuario BIGINT NOT NULL,
    id_rol BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuario_rol (id_usuario, id_rol),
    KEY idx_usuario_roles_usuario (id_usuario),
    CONSTRAINT fk_ur_rol FOREIGN KEY (id_rol) REFERENCES roles(id_rol)
);

INSERT IGNORE INTO permisos (id_permiso, nombre) VALUES
    (1, 'VER_CATALOGO'),
    (2, 'GESTIONAR_PEDIDOS'),
    (3, 'GESTIONAR_USUARIOS'),
    (4, 'GESTIONAR_STOCK');

INSERT IGNORE INTO roles (id_rol, nombre) VALUES
    (1, 'ROLE_CLIENTE'),
    (2, 'ROLE_ADMIN'),
    (3, 'ROLE_OPERADOR');

INSERT IGNORE INTO rol_permisos (id_rol, id_permiso) VALUES
    (1, 1),
    (2, 1), (2, 2), (2, 3), (2, 4),
    (3, 1), (3, 2), (3, 4);

-- NOTA: La asignacion de roles a usuarios se maneja via SecurityInitializer (configuracion)
-- que lee la propiedad 'security.admin.default-user-id' de application.yml
