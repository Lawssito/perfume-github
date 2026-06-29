CREATE DATABASE IF NOT EXISTS bd_auth_service;

CREATE DATABASE IF NOT EXISTS bd_user_service;

CREATE DATABASE IF NOT EXISTS bd_ms_carrito;

CREATE DATABASE IF NOT EXISTS bd_ms_catalogo;

CREATE DATABASE IF NOT EXISTS bd_ms_pedidos;

CREATE DATABASE IF NOT EXISTS bd_ms_pagos;

CREATE DATABASE IF NOT EXISTS bd_ms_envios;

CREATE DATABASE IF NOT EXISTS bd_ms_stock;

CREATE DATABASE IF NOT EXISTS bd_ms_notificaciones;

CREATE DATABASE IF NOT EXISTS bd_security_service;

USE bd_auth_service;
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

-- Password para ambos usuarios: 123456
INSERT INTO credenciales (id_credencial, id_usuario, email, password_hash, estado_cuenta)
SELECT 1, 1, 'admin@perfume.com', '$2a$10$.edOBzwVJZXNVer88qTN6OStO/TVt.CFGO.mYW/A1Lf3yUqFKkoXK', 'ACTIVO'
WHERE NOT EXISTS (SELECT 1 FROM credenciales WHERE id_credencial = 1);
INSERT INTO credenciales (id_credencial, id_usuario, email, password_hash, estado_cuenta)
SELECT 2, 2, 'cliente@perfume.com', '$2a$10$.edOBzwVJZXNVer88qTN6OStO/TVt.CFGO.mYW/A1Lf3yUqFKkoXK', 'ACTIVO'
WHERE NOT EXISTS (SELECT 1 FROM credenciales WHERE id_credencial = 2);

USE bd_user_service;
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

INSERT INTO usuarios (id_usuario, email, nombre, telefono, estado)
SELECT 1, 'admin@perfume.com', 'Admin Principal', '+56911111111', 'ACTIVO'
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE id_usuario = 1);
INSERT INTO usuarios (id_usuario, email, nombre, telefono, estado)
SELECT 2, 'cliente@perfume.com', 'Cliente Test', '+56922222222', 'ACTIVO'
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE id_usuario = 2);

USE bd_ms_catalogo;
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

INSERT INTO marcas (id_marca, nombre) SELECT 1, 'Chanel' WHERE NOT EXISTS (SELECT 1 FROM marcas WHERE id_marca = 1);
INSERT INTO marcas (id_marca, nombre) SELECT 2, 'Dior' WHERE NOT EXISTS (SELECT 1 FROM marcas WHERE id_marca = 2);
INSERT INTO categorias (id_categoria, nombre, descripcion) SELECT 1, 'Masculino', 'Fragancias para hombre' WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE id_categoria = 1);
INSERT INTO categorias (id_categoria, nombre, descripcion) SELECT 2, 'Femenino', 'Fragancias para mujer' WHERE NOT EXISTS (SELECT 1 FROM categorias WHERE id_categoria = 2);

INSERT INTO perfumes (id_perfume, id_categoria, id_marca, concentracion, nombre, descripcion)
SELECT 1, 1, 1, 'Eau de Toilette', 'Bleu de Chanel', 'Fragancia amaderada y aromatica' WHERE NOT EXISTS (SELECT 1 FROM perfumes WHERE id_perfume = 1);
INSERT INTO perfumes (id_perfume, id_categoria, id_marca, concentracion, nombre, descripcion)
SELECT 2, 2, 1, 'Eau de Parfum', 'Coco Mademoiselle', 'Fragancia floral oriental' WHERE NOT EXISTS (SELECT 1 FROM perfumes WHERE id_perfume = 2);
INSERT INTO perfumes (id_perfume, id_categoria, id_marca, concentracion, nombre, descripcion)
SELECT 3, 1, 2, 'Eau de Toilette', 'Sauvage', 'Fragancia fresca y audaz' WHERE NOT EXISTS (SELECT 1 FROM perfumes WHERE id_perfume = 3);
INSERT INTO perfumes (id_perfume, id_categoria, id_marca, concentracion, nombre, descripcion)
SELECT 4, 2, 2, 'Eau de Parfum', 'Jadore', 'Fragancia floral femenina' WHERE NOT EXISTS (SELECT 1 FROM perfumes WHERE id_perfume = 4);

INSERT INTO variantes (id_variante, id_perfume, sku, ml, precio)
SELECT 1, 1, 'CHANEL-BLEU-50', 50, 45000 WHERE NOT EXISTS (SELECT 1 FROM variantes WHERE id_variante = 1);
INSERT INTO variantes (id_variante, id_perfume, sku, ml, precio)
SELECT 2, 1, 'CHANEL-BLEU-100', 100, 75000 WHERE NOT EXISTS (SELECT 1 FROM variantes WHERE id_variante = 2);
INSERT INTO variantes (id_variante, id_perfume, sku, ml, precio)
SELECT 3, 2, 'CHANEL-COCO-50', 50, 55000 WHERE NOT EXISTS (SELECT 1 FROM variantes WHERE id_variante = 3);
INSERT INTO variantes (id_variante, id_perfume, sku, ml, precio)
SELECT 4, 2, 'CHANEL-COCO-100', 100, 85000 WHERE NOT EXISTS (SELECT 1 FROM variantes WHERE id_variante = 4);
INSERT INTO variantes (id_variante, id_perfume, sku, ml, precio)
SELECT 5, 3, 'DIOR-SAUVAGE-60', 60, 48000 WHERE NOT EXISTS (SELECT 1 FROM variantes WHERE id_variante = 5);
INSERT INTO variantes (id_variante, id_perfume, sku, ml, precio)
SELECT 6, 3, 'DIOR-SAUVAGE-100', 100, 78000 WHERE NOT EXISTS (SELECT 1 FROM variantes WHERE id_variante = 6);
INSERT INTO variantes (id_variante, id_perfume, sku, ml, precio)
SELECT 7, 4, 'DIOR-JADORE-50', 50, 52000 WHERE NOT EXISTS (SELECT 1 FROM variantes WHERE id_variante = 7);
INSERT INTO variantes (id_variante, id_perfume, sku, ml, precio)
SELECT 8, 4, 'DIOR-JADORE-100', 100, 82000 WHERE NOT EXISTS (SELECT 1 FROM variantes WHERE id_variante = 8);

USE bd_ms_carrito;
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

USE bd_ms_pedidos;
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

USE bd_ms_pagos;
CREATE TABLE IF NOT EXISTS transacciones (
    id_transaccion BIGINT NOT NULL AUTO_INCREMENT,
    id_pedido BIGINT NOT NULL,
    monto_total DECIMAL(10,2) NOT NULL,
    metodo_pago VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    referencia_externa VARCHAR(100),
    creado_en DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    procesado_en DATETIME,
    PRIMARY KEY (id_transaccion),
    UNIQUE KEY uk_transacciones_pedido (id_pedido),
    CONSTRAINT chk_monto_total CHECK (monto_total > 0)
);

USE bd_ms_envios;
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

USE bd_ms_stock;
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

INSERT INTO inventario (id_inventario, id_variante, cantidad_disponible, cantidad_reservada, version)
SELECT 1, 1, 100, 0, 0 WHERE NOT EXISTS (SELECT 1 FROM inventario WHERE id_inventario = 1);
INSERT INTO inventario (id_inventario, id_variante, cantidad_disponible, cantidad_reservada, version)
SELECT 2, 2, 50, 0, 0 WHERE NOT EXISTS (SELECT 1 FROM inventario WHERE id_inventario = 2);
INSERT INTO inventario (id_inventario, id_variante, cantidad_disponible, cantidad_reservada, version)
SELECT 3, 3, 80, 0, 0 WHERE NOT EXISTS (SELECT 1 FROM inventario WHERE id_inventario = 3);
INSERT INTO inventario (id_inventario, id_variante, cantidad_disponible, cantidad_reservada, version)
SELECT 4, 4, 40, 0, 0 WHERE NOT EXISTS (SELECT 1 FROM inventario WHERE id_inventario = 4);
INSERT INTO inventario (id_inventario, id_variante, cantidad_disponible, cantidad_reservada, version)
SELECT 5, 5, 120, 0, 0 WHERE NOT EXISTS (SELECT 1 FROM inventario WHERE id_inventario = 5);
INSERT INTO inventario (id_inventario, id_variante, cantidad_disponible, cantidad_reservada, version)
SELECT 6, 6, 60, 0, 0 WHERE NOT EXISTS (SELECT 1 FROM inventario WHERE id_inventario = 6);
INSERT INTO inventario (id_inventario, id_variante, cantidad_disponible, cantidad_reservada, version)
SELECT 7, 7, 90, 0, 0 WHERE NOT EXISTS (SELECT 1 FROM inventario WHERE id_inventario = 7);
INSERT INTO inventario (id_inventario, id_variante, cantidad_disponible, cantidad_reservada, version)
SELECT 8, 8, 30, 0, 0 WHERE NOT EXISTS (SELECT 1 FROM inventario WHERE id_inventario = 8);

USE bd_ms_notificaciones;
CREATE TABLE IF NOT EXISTS notificaciones (
    id_notificacion BIGINT NOT NULL AUTO_INCREMENT,
    id_usuario BIGINT NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado_envio VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    PRIMARY KEY (id_notificacion),
    KEY idx_notificaciones_usuario (id_usuario)
);

USE bd_security_service;
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

INSERT INTO permisos (id_permiso, nombre) SELECT 1, 'VER_CATALOGO' WHERE NOT EXISTS (SELECT 1 FROM permisos WHERE id_permiso = 1);
INSERT INTO permisos (id_permiso, nombre) SELECT 2, 'GESTIONAR_PEDIDOS' WHERE NOT EXISTS (SELECT 1 FROM permisos WHERE id_permiso = 2);
INSERT INTO permisos (id_permiso, nombre) SELECT 3, 'GESTIONAR_USUARIOS' WHERE NOT EXISTS (SELECT 1 FROM permisos WHERE id_permiso = 3);
INSERT INTO permisos (id_permiso, nombre) SELECT 4, 'GESTIONAR_STOCK' WHERE NOT EXISTS (SELECT 1 FROM permisos WHERE id_permiso = 4);

INSERT INTO roles (id_rol, nombre) SELECT 1, 'ROLE_CLIENTE' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE id_rol = 1);
INSERT INTO roles (id_rol, nombre) SELECT 2, 'ROLE_ADMIN' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE id_rol = 2);
INSERT INTO roles (id_rol, nombre) SELECT 3, 'ROLE_OPERADOR' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE id_rol = 3);

INSERT INTO rol_permisos (id_rol, id_permiso) SELECT 1, 1 WHERE NOT EXISTS (SELECT 1 FROM rol_permisos WHERE id_rol = 1 AND id_permiso = 1);
INSERT INTO rol_permisos (id_rol, id_permiso) SELECT 2, 1 WHERE NOT EXISTS (SELECT 1 FROM rol_permisos WHERE id_rol = 2 AND id_permiso = 1);
INSERT INTO rol_permisos (id_rol, id_permiso) SELECT 2, 2 WHERE NOT EXISTS (SELECT 1 FROM rol_permisos WHERE id_rol = 2 AND id_permiso = 2);
INSERT INTO rol_permisos (id_rol, id_permiso) SELECT 2, 3 WHERE NOT EXISTS (SELECT 1 FROM rol_permisos WHERE id_rol = 2 AND id_permiso = 3);
INSERT INTO rol_permisos (id_rol, id_permiso) SELECT 2, 4 WHERE NOT EXISTS (SELECT 1 FROM rol_permisos WHERE id_rol = 2 AND id_permiso = 4);
INSERT INTO rol_permisos (id_rol, id_permiso) SELECT 3, 1 WHERE NOT EXISTS (SELECT 1 FROM rol_permisos WHERE id_rol = 3 AND id_permiso = 1);
INSERT INTO rol_permisos (id_rol, id_permiso) SELECT 3, 2 WHERE NOT EXISTS (SELECT 1 FROM rol_permisos WHERE id_rol = 3 AND id_permiso = 2);
INSERT INTO rol_permisos (id_rol, id_permiso) SELECT 3, 4 WHERE NOT EXISTS (SELECT 1 FROM rol_permisos WHERE id_rol = 3 AND id_permiso = 4);

-- Asignacion directa de roles a usuarios (para que funcione aunque security-service
-- aun no haya arrancado o el SecurityInitializer no se haya ejecutado)
INSERT INTO usuario_roles (id_usuario, id_rol) SELECT 1, 1 WHERE NOT EXISTS (SELECT 1 FROM usuario_roles WHERE id_usuario = 1 AND id_rol = 1);
INSERT INTO usuario_roles (id_usuario, id_rol) SELECT 1, 2 WHERE NOT EXISTS (SELECT 1 FROM usuario_roles WHERE id_usuario = 1 AND id_rol = 2);
INSERT INTO usuario_roles (id_usuario, id_rol) SELECT 2, 1 WHERE NOT EXISTS (SELECT 1 FROM usuario_roles WHERE id_usuario = 2 AND id_rol = 1);