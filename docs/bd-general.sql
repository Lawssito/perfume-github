-- ============================================
-- BASE DE DATOS GENERAL - Perfume Platform
-- ============================================
-- Este script contiene la estructura de todas
-- las bases de datos utilizadas por los microservicios.
-- Cada microservicio con JPA gestiona su propio esquema.
-- ============================================

-- Crear bases de datos por microservicio
CREATE DATABASE IF NOT EXISTS db_auth_service;
CREATE DATABASE IF NOT EXISTS db_user_service;
CREATE DATABASE IF NOT EXISTS db_ms_carrito;
CREATE DATABASE IF NOT EXISTS db_ms_catalogo;
CREATE DATABASE IF NOT EXISTS db_ms_pedidos;
CREATE DATABASE IF NOT EXISTS db_ms_pagos;
CREATE DATABASE IF NOT EXISTS db_ms_envios;
CREATE DATABASE IF NOT EXISTS db_ms_stock;
CREATE DATABASE IF NOT EXISTS db_ms_notificaciones;
CREATE DATABASE IF NOT EXISTS db_security_service;

-- ============================================
-- TABLAS - auth-service
-- ============================================
USE db_auth_service;

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    rol VARCHAR(20) NOT NULL DEFAULT 'USER',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- TABLAS - user-service
-- ============================================
USE db_user_service;

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    telefono VARCHAR(20),
    direccion VARCHAR(255),
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- TABLAS - ms-catalogo
-- ============================================
USE db_ms_catalogo;

CREATE TABLE IF NOT EXISTS categorias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS productos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    precio DECIMAL(10, 2) NOT NULL,
    categoria_id BIGINT,
    imagen_url VARCHAR(255),
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (categoria_id) REFERENCES categorias(id)
);

-- ============================================
-- TABLAS - ms-carrito
-- ============================================
USE db_ms_carrito;

CREATE TABLE IF NOT EXISTS carritos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(20) DEFAULT 'ACTIVO'
);

CREATE TABLE IF NOT EXISTS carrito_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    carrito_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL DEFAULT 1,
    precio_unitario DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (carrito_id) REFERENCES carritos(id)
);

-- ============================================
-- TABLAS - ms-pedidos
-- ============================================
USE db_ms_pedidos;

CREATE TABLE IF NOT EXISTS pedidos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    fecha_pedido TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    total DECIMAL(12, 2) NOT NULL,
    direccion_envio VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS pedido_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id)
);

-- ============================================
-- TABLAS - ms-pagos
-- ============================================
USE db_ms_pagos;

CREATE TABLE IF NOT EXISTS pagos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    monto DECIMAL(12, 2) NOT NULL,
    metodo_pago VARCHAR(30) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_pago TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    transaction_id VARCHAR(100)
);

-- ============================================
-- TABLAS - ms-envios
-- ============================================
USE db_ms_envios;

CREATE TABLE IF NOT EXISTS envios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    direccion VARCHAR(255) NOT NULL,
    ciudad VARCHAR(100),
    codigo_postal VARCHAR(20),
    pais VARCHAR(50) DEFAULT 'Perú',
    estado VARCHAR(30) NOT NULL DEFAULT 'PREPARANDO',
    fecha_envio TIMESTAMP,
    fecha_entrega TIMESTAMP,
    empresa_envio VARCHAR(100)
);

-- ============================================
-- TABLAS - ms-stock
-- ============================================
USE db_ms_stock;

CREATE TABLE IF NOT EXISTS stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    producto_id BIGINT NOT NULL UNIQUE,
    cantidad_disponible INT NOT NULL DEFAULT 0,
    cantidad_reservada INT NOT NULL DEFAULT 0,
    stock_minimo INT NOT NULL DEFAULT 5,
    ultima_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================
-- TABLAS - ms-notificaciones
-- ============================================
USE db_ms_notificaciones;

CREATE TABLE IF NOT EXISTS notificaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    mensaje TEXT NOT NULL,
    leido BOOLEAN DEFAULT FALSE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- TABLAS - security-service
-- ============================================
USE db_security_service;

CREATE TABLE IF NOT EXISTS tokens_blacklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(500) NOT NULL,
    fecha_expiracion TIMESTAMP NOT NULL,
    fecha_invalido TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
