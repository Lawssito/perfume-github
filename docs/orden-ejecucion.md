# Perfume Platform - Orden de Ejecución

## Requisitos Previos

Antes de levantar el sistema se debe tener iniciado:

- Docker (contenedor MySQL) o XAMPP con MySQL en puerto 3307
- Eureka Server
- Microservicios de negocio y seguridad
- API Gateway

## Bases de Datos

El proyecto usa una base de datos independiente por microservicio:

| Microservicio      | Base de Datos           |
|--------------------|-------------------------|
| auth-service       | bd_auth_service         |
| user-service       | bd_user_service         |
| security-service   | bd_security_service     |
| ms-catalogo        | bd_ms_catalogo          |
| ms-carrito         | bd_ms_carrito           |
| ms-pedidos         | bd_ms_pedidos           |
| ms-pagos           | bd_ms_pagos             |
| ms-envios          | bd_ms_envios            |
| ms-stock           | bd_ms_stock             |
| ms-notificaciones  | bd_ms_notificaciones    |

El script de creación se encuentra en:

```text
docs/bd-general.sql
```

## Puertos

| Servicio            | Puerto |
|---------------------|--------|
| Eureka Server       | 8761   |
| API Gateway         | 8080   |
| auth-service        | 8082   |
| user-service        | 8081   |
| security-service    | 8083   |
| ms-catalogo         | 8084   |
| ms-stock            | 8085   |
| ms-carrito          | 8086   |
| ms-pagos            | 8087   |
| ms-envios           | 8088   |
| ms-notificaciones   | 8089   |
| ms-pedidos          | 8090   |
| MySQL               | 3307   |

## Orden de Arranque

1. **MySQL** (Docker o XAMPP)
2. **Eureka Server** (`eureka-server`)
3. **Microservicios** (auth-service, user-service, security-service, ms-catalogo, ms-stock, ms-carrito, ms-pagos, ms-envios, ms-notificaciones, ms-pedidos)
4. **API Gateway** (`api-gateway`)

## Perfiles de Configuración

Cada microservicio cuenta con perfiles YAML:

| Perfil | Archivo | Uso |
|--------|---------|-----|
| `dev` | `application-dev.yml` | Desarrollo: SQL visible, logs DEBUG |
| `prod` | `application-prod.yml` | Producción: SQL oculto, logs WARN/INFO |

Activar con: `--spring.profiles.active=prod` o variable `SPRING_PROFILES_ACTIVE=prod`.

## Arranque Local (Nativo)

```batch
arrancar-nativo.bat
```

## Arranque con Docker

Construir e iniciar todos los servicios:

```bash
docker compose build
docker compose up -d
```

Esto inicia: MySQL + Eureka Server + 10 microservicios + API Gateway.

Para construir un microservicio individual:

```bash
docker build -f ms-catalogo/Dockerfile -t ms-catalogo .
```

## Arranque Manual (Maven)

```bash
# 1. Eureka Server
cd eureka-server && .\mvnw.cmd spring-boot:run

# 2. Microservicios (en ventanas separadas)
cd ms-catalogo && .\mvnw.cmd spring-boot:run
cd ms-stock && .\mvnw.cmd spring-boot:run
# ... etc

# 3. API Gateway
cd api-gateway && .\mvnw.cmd spring-boot:run
```
