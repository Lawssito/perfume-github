# Perfume Store API

Backend para un e-commerce de perfumes desarrollado con **Java + Spring Boot**.
Este proyecto expone una API REST para gestionar catalogo, carrito, pedidos, pagos y autenticacion de usuarios.

## Tabla de contenidos

- [Descripcion](#descripcion)
- [Tecnologias](#tecnologias)
- [Arquitectura del proyecto](#arquitectura-del-proyecto)
- [Requisitos](#requisitos)
- [Configuracion](#configuracion)
- [Ejecucion local](#ejecucion-local)
- [Endpoints principales](#endpoints-principales)
- [Estructura sugerida](#estructura-sugerida)
- [Pruebas](#pruebas)
- [Despliegue](#despliegue)
- [Contribucion](#contribucion)
- [Licencia](#licencia)

## Descripcion

La API permite:

- Gestionar perfumes (catalogo, stock, precio, categorias, marcas).
- Registrar e iniciar sesion de clientes.
- Administrar carrito de compras.
- Generar y consultar pedidos.
- Integrar metodos de pago.
- Gestionar direcciones de envio.
- Mantener historial de compras.

## Tecnologias

- Java 17+
- Spring Boot 3+
- Spring Web
- Spring Data JPA
- Spring Security + JWT
- Bean Validation
- PostgreSQL (o MySQL)
- Maven o Gradle
- Docker (opcional)
- Swagger / OpenAPI (opcional)

## Arquitectura del proyecto

Se recomienda arquitectura por capas:

- **Controller**: expone endpoints REST.
- **Service**: contiene reglas de negocio.
- **Repository**: acceso a datos con JPA.
- **Domain/Entity**: modelos persistentes.
- **DTO/Mapper**: contratos de entrada/salida.
- **Security**: autenticacion y autorizacion.

## Requisitos

- JDK 17 o superior
- Maven 3.9+ (o Gradle equivalente)
- Base de datos PostgreSQL / MySQL
- Git

## Configuracion

Crear archivo de entorno (segun tu estrategia) y ajustar `application.yml` o `application.properties`.

### Variables de entorno sugeridas

```bash
APP_PORT=8080
DB_HOST=localhost
DB_PORT=5432
DB_NAME=perfume_store
DB_USER=postgres
DB_PASSWORD=postgres
JWT_SECRET=change_this_secret
JWT_EXPIRATION_MS=86400000
```

### Ejemplo de `application.yml`

```yaml
server:
  port: ${APP_PORT:8080}

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:perfume_store}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

## Ejecucion local

### 1) Clonar el repositorio

```bash
git clone <URL_DEL_REPOSITORIO>
cd <NOMBRE_DEL_PROYECTO>
```

### 2) Compilar el proyecto

Con Maven:

```bash
mvn clean install
```

Con Gradle:

```bash
./gradlew clean build
```

### 3) Levantar la aplicacion

Con Maven:

```bash
mvn spring-boot:run
```

Con Gradle:

```bash
./gradlew bootRun
```

La API quedara disponible en:

- `http://localhost:8080`

## Endpoints principales

> Ajusta las rutas segun tu implementacion real.

### Auth

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`

### Catalogo de perfumes

- `GET /api/v1/perfumes`
- `GET /api/v1/perfumes/{id}`
- `POST /api/v1/perfumes` (admin)
- `PUT /api/v1/perfumes/{id}` (admin)
- `DELETE /api/v1/perfumes/{id}` (admin)

### Carrito

- `GET /api/v1/cart`
- `POST /api/v1/cart/items`
- `PUT /api/v1/cart/items/{itemId}`
- `DELETE /api/v1/cart/items/{itemId}`

### Pedidos

- `POST /api/v1/orders`
- `GET /api/v1/orders`
- `GET /api/v1/orders/{id}`

## Estructura sugerida

```text
src/
  main/
    java/com/tuempresa/perfumestore/
      config/
      controller/
      service/
      repository/
      domain/
      dto/
      security/
      exception/
    resources/
      application.yml
  test/
```

## Pruebas

Ejecutar tests:

Con Maven:

```bash
mvn test
```

Con Gradle:

```bash
./gradlew test
```

## Despliegue

Opciones comunes:

- Docker + Docker Compose
- Kubernetes
- Plataformas cloud (AWS, GCP, Azure, Render, Railway, etc.)

Recomendaciones:

- Configurar perfiles (`dev`, `staging`, `prod`).
- Usar migraciones con Flyway o Liquibase.
- Centralizar logs y metricas.
- Configurar health checks (`/actuator/health`).

## Contribucion

1. Crear una rama feature: `git checkout -b feature/nueva-funcionalidad`
2. Realizar cambios y commits descriptivos.
3. Abrir Pull Request con contexto funcional y tecnico.

## Licencia

Este proyecto se distribuye bajo la licencia que defina el equipo.
