# 🚀 Perfume Store — Microservicios Multimódulo

**Estudiantes:** Andres Cereño y Julian Silva

Sistema de e-commerce de perfumes basado en una arquitectura de **microservicios** con **Java 21**, **Spring Boot 4.0.6** y **Spring Cloud 2025.1.x**. Cada dominio de negocio vive en un microservicio independiente con su propia base de datos MySQL, comunicándose vía OpenFeign y descubriéndose a través de Eureka.

## 📦 Componentes de distribución

| Componente | Descripción | Enlace |
|------------|-------------|--------|
| 🐳 ZIP Con Docker | JARs + docker-compose + scripts para despliegue con Docker | [Descargar ZIP Con Docker](https://drive.google.com/file/d/1b8X8NVXkq84MkW91DxbUSO8k8pb3QvgS/view?usp=sharing) |
| 🎥 Video Defensa Técnica | Video explicativo del sistema (10 min) | [Ver Video](https://drive.google.com/file/d/1piwypMrOSPDqUi5mZ2O2WZPGYXZGlnFb/view?usp=sharing) |
| 📄 Subtítulos del Video | Transcripción del video de defensa | [Guión](https://drive.google.com/file/d/1IRpdBip9o1cYDpLxJq7wj3Ec20qWuKUh/view?usp=sharing) |

---

## Tabla de contenidos

- [Descripción](#descripción)
- [Tecnologías](#tecnologías)
- [Arquitectura](#arquitectura)
- [Microservicios](#microservicios)
- [API Gateway y rutas](#api-gateway-y-rutas)
- [Endpoints principales](#endpoints-principales)
- [Swagger](#swagger)
- [Requisitos](#requisitos)
- [Ejecución con Docker](#ejecución-con-docker)
- [Flujo de compra](#flujo-de-compra-carrito--pedido--pago)
- [Pruebas unitarias](#pruebas-unitarias-junit-5--mockito)
- [JaCoCo](#jacoco)
- [Estructura del repositorio](#estructura-del-repositorio)

## Descripción

El sistema permite:

- Gestionar catálogo (perfumes, variantes, marcas y categorías).
- Registrar usuarios y administrar direcciones de envío.
- Autenticación con JWT (login y validación de token).
- Roles y permisos centralizados.
- Carrito de compras, pedidos, pagos y envíos.
- Control de inventario y notificaciones.

Cada dominio vive en un microservicio independiente con su propia base de datos MySQL.

## Tecnologías

- Java 21
- Spring Boot 4.0.x
- Spring Cloud 2025.1.x (Eureka, Gateway Server WebMVC, OpenFeign, LoadBalancer)
- Spring Data JPA
- Spring Security + JWT (auth-service)
- Bean Validation
- MySQL
- Maven
- SpringDoc OpenAPI (Swagger UI en varios servicios)

## Arquitectura

```text
Cliente (Postman / Frontend)
        │
        ▼
  API Gateway :8080
        │
        ▼
  Eureka Server :8761  ◄── registro y descubrimiento
        │
        ├── auth-service
        ├── user-service
        ├── security-service
        ├── ms-catalogo
        ├── ms-stock
        ├── ms-carrito
        ├── ms-pagos
        ├── ms-envios
        ├── ms-notificaciones
        └── ms-pedidos
```

- **Eureka Server**: registro de instancias de cada microservicio.
- **API Gateway**: punto de entrada único en el puerto `8080`. Enruta con `lb://<nombre-servicio>` hacia las instancias registradas en Eureka.
- **Microservicios**: cada uno expone REST en su puerto y se comunica con otros vía OpenFeign cuando corresponde.

Cada microservicio sigue capas internas: `controller` → `service` → `repository`, con `dto`, `model` y `exception`.

## Microservicios

El sistema está compuesto por **12 microservicios** que cubren todo el ciclo de vida de un e-commerce:

| Microservicio | Función |
|---------------|---------|
| `eureka-server` | Servidor de descubrimiento y registro de servicios |
| `api-gateway` | Puerta de entrada única (Gateway Server WebMVC) |
| `auth-service` | Autenticación y gestión de credenciales (JWT) |
| `user-service` | Gestión de usuarios y direcciones |
| `security-service` | Roles, permisos y autorización (RBAC) |
| `ms-catalogo` | Catálogo de perfumes, marcas, categorías y variantes |
| `ms-stock` | Control de inventario y stock |
| `ms-carrito` | Carrito de compras por usuario |
| `ms-pedidos` | Orquestación de pedidos (orquesta carrito, stock, pago, envío) |
| `ms-pagos` | Procesamiento de pagos (simulación bancaria) |
| `ms-envios` | Gestión de envíos y couriers |
| `ms-notificaciones` | Notificaciones a usuarios |

### Puertos

| Servicio | Puerto | Nombre en Eureka | Prefijo API |

| Servicio | Puerto | Nombre en Eureka | Prefijo API |
|----------|--------|------------------|-------------|
| Eureka Server | 8761 | — | — |
| API Gateway | **8080** | api-gateway | Todas las rutas `/api/**` |
| user-service | 8081 | user-service | `/api/usuarios` |
| auth-service | 8082 | auth-service | `/api/auth` |
| security-service | 8083 | security-service | `/api/usuario-roles`, `/api/security` |
| ms-catalogo | 8084 | ms-catalogo | `/api/catalogo` |
| ms-stock | 8085 | ms-stock | `/api/stock` |
| ms-carrito | 8086 | ms-carrito | `/api/carrito` |
| ms-pagos | 8087 | ms-pagos | `/api/pagos` |
| ms-envios | 8088 | ms-envios | `/api/envios` |
| ms-notificaciones | 8089 | ms-notificaciones | `/api/notificaciones` |
| ms-pedidos | 8090 | ms-pedidos | `/api/pedidos` |

> **Recomendado:** usar siempre el **API Gateway** (`http://localhost:8080`) para las pruebas. Los puertos individuales sirven para depuración directa de cada servicio.

## Requisitos

- JDK 21
- Maven 3.9+ (solo para compilar)
- Docker Desktop
- Git

## Ejecución con Docker

### 1. Clonar el repositorio

```bash
git clone https://github.com/Lawssito/perfume-github.git
cd perfume-github
```

### 2. Compilar

```bash
mvn clean package -DskipTests
```

### 3. Iniciar todos los servicios

```bash
docker compose up -d
```

Esto levanta **13 contenedores**: MySQL 8.0, Eureka Server, API Gateway y los 10 microservicios.

### 4. Verificar

- **Eureka Dashboard:** `http://localhost:8761` — todas las instancias `UP`
- **API Gateway:** `http://localhost:8080`

### 5. Detener

```bash
docker compose down
```

## API Gateway y rutas

El gateway en el puerto **8080** reenvía según el path:

| Ruta | Microservicio |
|------|---------------|
| `/api/auth/**` | auth-service |
| `/api/usuarios/**` | user-service |
| `/api/usuario-roles/**` | security-service |
| `/api/security/**` | security-service |
| `/api/catalogo/**` | ms-catalogo |
| `/api/stock/**` | ms-stock |
| `/api/carrito/**` | ms-carrito |
| `/api/pagos/**` | ms-pagos |
| `/api/envios/**` | ms-envios |
| `/api/notificaciones/**` | ms-notificaciones |
| `/api/pedidos/**` | ms-pedidos |

## Endpoints principales

Todas las URLs siguientes usan el gateway: `http://localhost:8080`.

### Autenticación (`auth-service`)

- `POST /api/auth/login` — Iniciar sesión (devuelve JWT)
- `POST /api/auth/validate` — Validar token
- `POST /api/auth/credenciales` — Crear credenciales
- `GET /api/auth/credenciales/usuario/{idUsuario}`

### Usuarios (`user-service`)

- `POST /api/usuarios` — Registrar usuario
- `GET /api/usuarios` — Listar usuarios
- `GET /api/usuarios/{id}` — Obtener por ID
- `PUT /api/usuarios/{id}` — Actualizar
- `DELETE /api/usuarios/{id}` — Eliminar
- `POST /api/usuarios/{id}/direcciones` — Agregar dirección
- `GET /api/usuarios/{id}/direcciones` — Listar direcciones

### Catálogo (`ms-catalogo`)

- `GET /api/catalogo/marcas`
- `GET /api/catalogo/categorias`
- `GET /api/catalogo/productos`
- `GET /api/catalogo/perfumes/{id}`
- `POST /api/catalogo/perfumes`
- `GET /api/catalogo/variantes/{id}`

### Seguridad (`security-service`)

- `POST /api/usuario-roles` — Asignar rol a usuario
- `GET /api/usuario-roles/{idUsuario}`
- `GET /api/security/roles`
- `POST /api/security/validar-permiso`

### Carrito (`ms-carrito`)

- `GET /api/carrito/{idUsuario}` — Obtener carrito (lo crea vacío si no existe)
- `POST /api/carrito/{idUsuario}/items` — Agregar variante (`idVariante`, `cantidad`)
- `PUT /api/carrito/{idUsuario}/items/{idItem}` — Actualizar cantidad
- `DELETE /api/carrito/{idUsuario}/items/{idItem}` — Quitar un ítem
- `DELETE /api/carrito/{idUsuario}` — Vaciar carrito (lo invoca `ms-pedidos` tras pago exitoso)

Al agregar un ítem, `ms-carrito` consulta **ms-catalogo** (precio) y **ms-stock** (disponibilidad).

### Stock (`ms-stock`)

- `GET /api/stock` — Listar inventario (`idVariante`, `cantidadDisponible`)
- `GET /api/stock/{idVariante}` — Consultar por variante
- `POST /api/stock/{idVariante}` — Crear registro de inventario
- `PUT /api/stock/{idVariante}/reponer` — Ingresar unidades
- `PUT /api/stock/{idVariante}/reducir` — Descontar unidades

> `cantidadReservada` existe en el modelo pero **no se usa** en la lógica actual; solo se expone en las respuestas (siempre `0` salvo cambios manuales en BD).

### Pedidos (`ms-pedidos`)

- `POST /api/pedidos` — Confirmar compra (orquesta carrito, catálogo, stock, pago, envío y notificaciones)
- `GET /api/pedidos` — Listar todos
- `GET /api/pedidos/usuario/{idUsuario}` — Historial por usuario
- `GET /api/pedidos/{id}` — Detalle de un pedido
- `PUT /api/pedidos/{id}/estado?estado=...` — Avanzar estado (bodega/admin)

### Pagos (`ms-pagos`)

- `POST /api/pagos` — Crear transacción (`PENDIENTE`)
- `POST /api/pagos/{id}/procesar` — Simular pasarela (~80% `COMPLETADO`, ~20% `RECHAZADO`)
- `GET /api/pagos/pedido/{idPedido}` — Consultar pago de un pedido

En el checkout normal el cliente **no** llama a estos endpoints: `ms-pedidos` los invoca por Feign al confirmar el pedido. Sirven para pruebas aisladas del dominio de pagos.

### Envíos y notificaciones

- **Envíos:** `/api/envios/**` (ms-envios)
- **Notificaciones:** `/api/notificaciones/**` (ms-notificaciones)

## Flujo de compra (carrito → pedido → pago)

`ms-pedidos` actúa como **orquestador**: un solo `POST /api/pedidos` dispara el proceso completo vía OpenFeign.

```text
1. GET  /api/catalogo/productos     → ver idVariante, SKU y precio
2. POST /api/stock/{idVariante}     → crear inventario (si no existe)
3. PUT  /api/stock/{idVariante}/reponer
4. POST /api/carrito/{idUsuario}/items
5. POST /api/pedidos                → confirmar compra
```

**Qué hace `POST /api/pedidos` internamente:**

1. Lee el carrito y valida que tenga ítems y total mayor a 0.
2. Verifica stock en **ms-stock** (falla si no hay inventario o stock insuficiente).
3. Consulta cada variante en **ms-catalogo** (SKU, ml) y guarda el pedido en estado `CREADO`.
4. Crea y procesa el pago en **ms-pagos**.
5. Si el pago es `COMPLETADO`: reduce stock, pasa el pedido a `PAGADO`, crea envío, notifica y **vacía el carrito**.
6. Si el pago es `RECHAZADO`: pedido `CANCELADO` y el **carrito se mantiene** para reintentar.

**Body de confirmación:**

```json
{
  "idUsuario": 1,
  "direccionEntrega": "Av. Providencia 1234, Santiago",
  "courier": "CHILEXPRESS"
}
```

**Servicios necesarios para el happy path:** `ms-catalogo`, `ms-stock`, `ms-carrito`, `ms-pagos` (y opcionalmente `ms-envios`, `ms-notificaciones`).

## Swagger

Cada microservicio expone su documentación interactiva vía **SpringDoc OpenAPI**. Se accede directamente al puerto del servicio:

| Servicio | Swagger UI |
|----------|------------|
| user-service | `http://localhost:8081/swagger-ui.html` |
| auth-service | `http://localhost:8082/swagger-ui.html` |
| security-service | `http://localhost:8083/swagger-ui.html` |
| ms-catalogo | `http://localhost:8084/swagger-ui.html` |
| ms-stock | `http://localhost:8085/swagger-ui.html` |
| ms-carrito | `http://localhost:8086/swagger-ui.html` |
| ms-pagos | `http://localhost:8087/swagger-ui.html` |
| ms-envios | `http://localhost:8088/swagger-ui.html` |
| ms-notificaciones | `http://localhost:8089/swagger-ui.html` |
| ms-pedidos | `http://localhost:8090/swagger-ui.html` |

## Pruebas con Postman

1. Levanta **Eureka**, **API Gateway** y el microservicio que vayas a probar.
2. Usa la URL base del gateway: `http://localhost:8080`.
3. Para endpoints protegidos, primero haz login y envía el header:

   ```http
   Authorization: Bearer <token>
   ```

**Ejemplos:**

```http
POST http://localhost:8080/api/usuarios
POST http://localhost:8080/api/auth/login
GET  http://localhost:8080/api/catalogo/productos
POST http://localhost:8080/api/carrito/1/items
POST http://localhost:8080/api/pedidos
```

Para descubrir `idVariante` en una BD con datos previos, usa `GET /api/catalogo/productos` o `GET /api/stock`.

Si recibes **404** en el puerto 8080 pero el mismo path funciona en el puerto directo del servicio (por ejemplo 8081), verifica que el servicio esté registrado en Eureka y que el api-gateway se haya reiniciado tras cambios en su configuración.

## Estructura del repositorio

```text
perfume-github/
├── eureka-server/       # Descubrimiento de servicios
├── api-gateway/         # Puerta de entrada (8080)
├── auth-service/
├── user-service/
├── security-service/
├── ms-catalogo/
├── ms-stock/
├── ms-carrito/
├── ms-pagos/
├── ms-envios/
├── ms-notificaciones/
├── ms-pedidos/
└── README.md
```

Cada módulo es un proyecto Maven independiente con su propio `pom.xml` y `src/main/java`.

## Pruebas unitarias (JUnit 5 + Mockito)

La suite de pruebas unitarias usa **JUnit 5** y **Mockito** para tests de controllers (`MockMvc`) y servicios. Para ejecutarlas:

```bash
mvn clean install
```

## JaCoCo

JaCoCo está configurado en el `pom.xml` raíz para generar reportes de cobertura. Los reportes se generan en `target/site/jacoco/index.html` de cada módulo.
