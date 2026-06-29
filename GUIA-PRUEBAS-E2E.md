# Guía de Pruebas End-to-End (E2E)

> **Arquitectura:** API Gateway en `http://localhost:8080` — todas las peticiones pasan por este puerto.
> Los microservicios internos NO son accesibles directamente desde el cliente.

---

## Requisitos previos

| Servicio | Puerto | Estado requerido |
|----------|--------|------------------|
| `eureka-server` | 8761 | Encendido |
| `api-gateway` | 8080 | Encendido |
| `auth-service` | 8082 | Encendido |
| `user-service` | 8081 | Encendido |
| `ms-catalogo` | 8084 | Encendido |
| `ms-stock` | 8085 | Encendido |
| `ms-carrito` | 8086 | Encendido |
| `ms-pagos` | 8087 | Encendido |
| `ms-pedidos` | 8090 | Encendido |
| `ms-envios` | 8088 | Encendido |
| `ms-notificaciones` | 8089 | Encendido |
| `security-service` | 8083 | Encendido |

> **Arquitectura de seguridad:** `security-service` ahora tiene **filtro JWT** propio. Las llamadas externas deben ir por el Gateway (puerto 8080) con token válido. Las llamadas internas entre servicios (Feign) usan una **API key interna** (`X-Internal-Api-Key`). El primer usuario registrado (`id_usuario=1`) obtiene `ROLE_ADMIN` automáticamente mediante seed en la BD.
>
> **Nuevo esquema de autorización (RBAC + Ownership):**
> - **Gateway** agrega headers `X-User-Id`, `X-User-Roles`, `X-User-Email` desde el JWT para que los microservicios puedan validar permisos sin depender del token directamente.
> - **ADMIN** puede: todo (operaciones de escritura en todos los servicios, listados globales, gestión de roles).
> - **CLIENTE** puede: gestionar su propio carrito y pedidos, ver sus notificaciones, consultar catálogo público.
> - **Sin token**: solo lectura pública (catálogo, stock consulta) y registro/login.
> - **Microservicios internos**: las llamadas Feign entre servicios no pasan por el Gateway, por lo que usan `X-Internal-Api-Key` para autenticarse entre sí.

---

## Etapa 0: Poblar Catálogo e Inventario

> **IMPORTANTE:** Realiza esta etapa **antes** del flujo principal para tener datos de prueba.
> El catálogo es **público** — no requiere token. Para las operaciones de escritura (crear perfume, variantes) se necesita token de **ADMIN**.

---

### 0.1 — Verificar marcas existentes

Las marcas `Chanel` y `Dior` ya vienen precargadas por `schema.sql`. Verifica que existan:

```http
GET http://localhost:8080/api/catalogo/marcas
```

**Respuesta esperada — `200 OK`:**
```json
[
  { "idMarca": 1, "nombre": "Chanel" },
  { "idMarca": 2, "nombre": "Dior" }
]
```

> Si no aparecen, créalas manualmente:

> ```http
> POST http://localhost:8080/api/catalogo/marcas
> Content-Type: application/json
> 
> { "nombre": "Chanel" }
> ```

---

### 0.2 — Verificar categorías existentes

```http
GET http://localhost:8080/api/catalogo/categorias
```

**Respuesta esperada — `200 OK`:**
```json
[
  { "idCategoria": 1, "nombre": "Masculino", "descripcion": "Fragancias para hombre" },
  { "idCategoria": 2, "nombre": "Femenino", "descripcion": "Fragancias para mujer" }
]
```

---

### 0.3 — Crear perfume con variantes

> **NOTA:** Si prefieres evitar registrarte, puedes usar los datos semilla que ya vienen en `schema.sql`. Pero para un entorno limpio o pruebas repetibles, crea tus propios perfumes:

Primero necesitas un token de **ADMIN**. El primer usuario registrado (`id_usuario=1`) obtiene automáticamente `ROLE_ADMIN` gracias al seed en la base de datos del `security-service`. Solo debes registrar al usuario y luego iniciar sesión.

### 0.3.1 — Registrar usuario candidato a admin

```http
POST http://localhost:8080/api/usuarios
Content-Type: application/json

{
  "email":    "admin@test.com",
  "nombre":   "Admin Test",
  "telefono": "5511111111",
  "password": "Admin123!"
}
```

**Respuesta esperada — `201 Created`:**
```json
{
  "idUsuario": 1,
  "email": "admin@test.com",
  "nombre": "Admin Test",
  "telefono": "5511111111",
  "estado": "ACTIVO"
}
```

> **GUARDA** `idUsuario` (ej. `1`).

### 0.3.2 — Rol ADMIN asignado según configuración

El `security-service` tiene un **inicializador** que lee la propiedad `security.admin.default-user-id` de `application.yml` (por defecto: `1`). Al arrancar, asigna `ROLE_CLIENTE` y `ROLE_ADMIN` al usuario con ese ID.

Esto significa que **el usuario que esperas que sea admin debe registrarse primero** para obtener `id_usuario=1`. Si registras primero a un cliente normal, ese cliente terminará con `ROLE_ADMIN`.

> **Para cambiar quién es admin:** edita `security-service/src/main/resources/application.yml` y modifica:
> ```yaml
> security:
>   admin:
>     default-user-id: 1  # ← cambia este ID o pon 0 para deshabilitar la auto-asignación
> ```
> 
> Si ya hay un admin en el sistema, puedes asignar `ROLE_ADMIN` a otros usuarios mediante el Gateway (puerto 8080) con token de administrador.

### 0.3.3 — Login como Admin

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email":    "admin@test.com",
  "password": "Admin123!"
}
```

**Respuesta esperada — `200 OK`  (token con rol ADMIN):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "idUsuario": 1,
  "email": "admin@test.com",
  "roles": ["ROLE_CLIENTE", "ROLE_ADMIN"],
  "mensaje": "Inicio de sesion exitoso",
  "refreshToken": "..."
}
```

> **GUARDA este token** para crear perfumes, variantes y stock.

**Crear perfume:**
```http
POST http://localhost:8080/api/catalogo/perfumes
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "idCategoria": 1,
  "idMarca": 1,
  "concentracion": "EDP",
  "nombre": "Bleu de Prueba",
  "descripcion": "Perfume de prueba para E2E"
}
```

**Respuesta esperada — `201 Created`:**
```json
{
  "idPerfume": 1,
  "nombre": "Bleu de Prueba",
  "concentracion": "EDP",
  "descripcion": "Perfume de prueba para E2E",
  "estado": "ACTIVO",
  "idMarca": 1,
  "nombreMarca": "Chanel",
  "idCategoria": 1,
  "nombreCategoria": "Masculino",
  "variantes": []
}
```

> **GUARDA** `idPerfume` (ej. `1`).

**Crear segundo perfume:**
```http
POST http://localhost:8080/api/catalogo/perfumes
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "idCategoria": 2,
  "idMarca": 2,
  "concentracion": "EDP",
  "nombre": "J'adore Test",
  "descripcion": "Perfume floral de pruebas"
}
```

**Respuesta esperada — `201 Created`:**
```json
{
  "idPerfume": 2,
  "nombre": "J'adore Test",
  "concentracion": "EDP",
  "descripcion": "Perfume floral de pruebas",
  "estado": "ACTIVO",
  "idMarca": 2,
  "nombreMarca": "Dior",
  "idCategoria": 2,
  "nombreCategoria": "Femenino",
  "variantes": []
}
```

---

### 0.4 — Agregar variantes a los perfumes

```http
POST http://localhost:8080/api/catalogo/perfumes/1/variantes
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "sku": "BLEU-50",
  "ml": 50,
  "precio": 899.00
}
```

**Respuesta esperada — `201 Created`:**
```json
{
  "idVariante": 1,
  "idPerfume": 1,
  "sku": "BLEU-50",
  "ml": 50,
  "precio": 899.00
}
```

Agrega una segunda variante al mismo perfume:
```http
POST http://localhost:8080/api/catalogo/perfumes/1/variantes
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "sku": "BLEU-100",
  "ml": 100,
  "precio": 1499.00
}
```

**Respuesta esperada — `201 Created`:**
```json
{
  "idVariante": 2,
  "idPerfume": 1,
  "sku": "BLEU-100",
  "ml": 100,
  "precio": 1499.00
}
```

Agrega una variante para el segundo perfume:
```http
POST http://localhost:8080/api/catalogo/perfumes/2/variantes
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "sku": "JADORE-75",
  "ml": 75,
  "precio": 1299.00
}
```

**Respuesta esperada — `201 Created`:**
```json
{
  "idVariante": 3,
  "idPerfume": 2,
  "sku": "JADORE-75",
  "ml": 75,
  "precio": 1299.00
}
```

> **GUARDA los** `idVariante` (1, 2, 3) — los usarás en el carrito.

---

### 0.5 — Crear inventario y reponer stock inicial

Primero crea el registro de inventario para cada variante (el stock arranca en 0):

```http
POST http://localhost:8080/api/stock/1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Respuesta esperada — `201 Created`:**
```json
{
  "idInventario": 1,
  "idVariante": 1,
  "cantidadDisponible": 0
}
```

Repite para las otras variantes:
```http
POST http://localhost:8080/api/stock/2
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

```http
POST http://localhost:8080/api/stock/3
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Ahora repón stock** para que haya inventario disponible:
```http
PUT http://localhost:8080/api/stock/1/reponer
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "cantidad": 20
}
```

**Respuesta esperada — `200 OK`:**
```json
{
  "idInventario": 1,
  "idVariante": 1,
  "cantidadDisponible": 20
}
```

Repón stock para las demás variantes:
```http
PUT http://localhost:8080/api/stock/2/reponer
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{ "cantidad": 15 }
```

```http
PUT http://localhost:8080/api/stock/3/reponer
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{ "cantidad": 10 }
```

**Catalogo e inventario listos.** Ahora puedes ejecutar el flujo completo con datos reales.

---

## Etapa 1: Flujo Completo (Happy Path)

Recorrido completo: **Registro → Login → Crear carrito → Agregar items → Crear pedido → Pagar** (pagar ahora hace todo automáticamente: confirma stock, crea envío, vacía carrito, notifica)

---

### 1.1 — Registro de usuario

Registra un cliente nuevo. Endpoint público (no requiere token).

```http
POST http://localhost:8080/api/usuarios
Content-Type: application/json

{
  "email":    "cliente@test.com",
  "nombre":   "Cliente Test",
  "telefono": "5512345678",
  "password": "Password123!"
}
```

**Respuesta esperada — `201 Created`:**
```json
{
  "idUsuario": 1,
  "email": "cliente@test.com",
  "nombre": "Cliente Test",
  "telefono": "5512345678",
  "estado": "ACTIVO"
}
```

> **GUARDA** `idUsuario` (ej. `1`) — lo usarás en todos los pasos siguientes.

---

### 1.2 — Login (obtener JWT)

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email":    "cliente@test.com",
  "password": "Password123!"
}
```

**Respuesta esperada — `200 OK`:**
```json
{
  "token":        "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType":    "Bearer",
  "idUsuario":    1,
  "email":        "cliente@test.com",
  "roles":        ["ROLE_CLIENTE"],
  "mensaje":      "Inicio de sesion exitoso",
  "refreshToken": "dGhpcyBpcyBh...refresh..."
}
```

> **GUARDA** `token` y `refreshToken` — los usarás en todos los pasos siguientes.
> A partir de aquí, **todas las llamadas** deben incluir el header:
> `Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...`

---

### 1.3 — Consultar catálogo (público)

```http
GET http://localhost:8080/api/catalogo/productos
```

**Respuesta esperada — `200 OK`:**
```json
[
  {
    "idPerfume": 1,
    "nombre": "Bleu de Prueba",
    "concentracion": "EDP",
    "descripcion": "Perfume de prueba para E2E",
    "estado": "ACTIVO",
    "idMarca": 1,
    "nombreMarca": "Chanel",
    "idCategoria": 1,
    "nombreCategoria": "Masculino",
    "variantes": [
      { "idVariante": 1, "idPerfume": 1, "sku": "BLEU-50", "ml": 50, "precio": 899.00 },
      { "idVariante": 2, "idPerfume": 1, "sku": "BLEU-100", "ml": 100, "precio": 1499.00 }
    ]
  }
]
```

> **GUARDA algun** `idVariante` (ej. `1`) para usarlo en el carrito.

---

### 1.4 — Obtener/Crear carrito

```http
GET http://localhost:8080/api/carrito/mi-carrito
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Respuesta esperada — `200 OK`:**
```json
{
  "idCarrito": 1,
  "idUsuario": 1,
  "items": [],
  "total": 0
}
```

---

### 1.5 — Agregar item al carrito

```http
POST http://localhost:8080/api/carrito/items
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "idVariante": 1,
  "cantidad": 2
}
```

**Respuesta esperada — `200 OK`:**
```json
{
  "idCarrito": 1,
  "idUsuario": 1,
  "items": [
    {
      "idItem": 1,
      "idVariante": 1,
      "cantidad": 2,
      "precioUnitario": 899.00,
      "subtotal": 1798.00
    }
  ],
  "total": 1798.00
}
```

> Si tienes más variantes con stock, repite este paso para tener 2–3 items en el carrito.

---

### 1.6 — Crear pedido (estado `CREADO`)

```http
POST http://localhost:8080/api/pedidos
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "direccionEntrega": "Calle Principal 123, Ciudad",
  "courier": "DHL"
}
```

**Respuesta esperada — `201 Created`:**
```json
{
  "idPedido": 1,
  "idUsuario": 1,
  "estado": "CREADO",
  "total": 1798.00,
  "fechaCreacion": "2026-06-17T12:00:00",
  "direccionEntrega": "Calle Principal 123, Ciudad",
  "courier": "DHL",
  "detalles": [
    {
      "idDetalle": 1,
      "idVariante": 1,
      "nombreProducto": "BLEU-50",
      "ml": 50,
      "cantidad": 2,
      "precioUnitario": 899.00,
      "subtotal": 1798.00
    }
  ]
}
```

> **GUARDA** `idPedido` (ej. `1`).

---

### 1.7 — Pagar pedido (`CREADO → EN_PREPARACION`)

Procesa el pago con tarjeta y ejecuta **todo** automáticamente. Internamente el sistema:
1. Crea un pago en `ms-pagos` (método `TARJETA`)
2. Procesa el pago con el simulador bancario
3. Si es `COMPLETADO`, ejecuta automáticamente:
   - **Confirma la reserva de stock** en `ms-stock` (descuento definitivo)
   - **Crea el envío** en `ms-envios`
   - **Vacía el carrito** en `ms-carrito`
   - **Envía notificación** en `ms-notificaciones`
   - Cambia el pedido a `EN_PREPARACION`
4. Si el pago es rechazado, el pedido queda en `CREADO` para reintentar

> **Ya no es necesario llamar a `/confirmar`.** El paso 1.7 hace todo.
> El endpoint `/confirmar` se conserva solo para pedidos legacy que quedaron en estado `PAGADO`.

```http
POST http://localhost:8080/api/pedidos/1/pagar
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Respuesta esperada — `200 OK`:**
```json
{
  "idPedido": 1,
  "idUsuario": 1,
  "estado": "EN_PREPARACION",
  "total": 1798.00,
  ...
}
```

**Happy path completado.** El pedido paso por: `CREADO -> EN_PREPARACION`.

---

## Etapa 2: Control de Acceso (RBAC + Ownership)

Esta etapa verifica que cada endpoint respete las reglas de autorización. Necesitarás **dos tokens**:
- **Token CLIENTE** — del usuario `cliente@test.com` (paso 1.2) con rol `ROLE_CLIENTE`
- **Token ADMIN** — del usuario `admin@test.com` (paso 0.3.3) con rol `ROLE_ADMIN`

> Si el cliente y el admin son el mismo usuario (`id_usuario=1`), registra un segundo usuario
> (`cliente2@test.com` / `Password123!`) para probar los escenarios de ownership.

---

### 2.1 — Matriz de pruebas de autorización

Para cada endpoint sensible, prueba **CLIENTE → 403** y **ADMIN → 200/204**.

#### Catálogo (ms-catalogo) — solo ADMIN

| Endpoint | CLIENTE | ADMIN |
|----------|---------|-------|
| `POST /api/catalogo/perfumes` | `403` | `201` |
| `PUT /api/catalogo/perfumes/1` | `403` | `200` |
| `DELETE /api/catalogo/perfumes/1` | `403` | `204` |
| `POST /api/catalogo/perfumes/1/variantes` | `403` | `201` |
| `PUT /api/catalogo/variantes/1` | `403` | `200` |
| `DELETE /api/catalogo/variantes/1` | `403` | `204` |
| `POST /api/catalogo/marcas` | `403` | `201` |
| `PUT /api/catalogo/marcas/1` | `403` | `200` |
| `DELETE /api/catalogo/marcas/1` | `403` | `204` |
| `POST /api/catalogo/categorias` | `403` | `201` |
| `PUT /api/catalogo/categorias/1` | `403` | `200` |
| `DELETE /api/catalogo/categorias/1` | `403` | `204` |

**Ejemplo — CLIENTE intenta crear perfume:**
```http
POST http://localhost:8080/api/catalogo/perfumes
Authorization: Bearer eyJ...  ← token CLIENTE
Content-Type: application/json

{
  "idCategoria": 1,
  "idMarca": 1,
  "concentracion": "EDP",
  "nombre": "Intento no autorizado"
}
```
**Respuesta — `403 Forbidden`:**
```json
{
  "status": 403,
  "error": "FORBIDDEN",
  "message": "No tienes permisos de administrador para realizar esta accion"
}
```

---

#### Stock (ms-stock) — solo ADMIN

| Endpoint | CLIENTE | ADMIN |
|----------|---------|-------|
| `POST /api/stock/1` | `403` | `201` |
| `PUT /api/stock/1/reponer` | `403` | `200` |
| `PUT /api/stock/1/reducir` | `403` | `200` |
| `PUT /api/stock/1/reservar` | `403` | `200` |
| `PUT /api/stock/1/confirmar-reserva` | `403` | `200` |
| `PUT /api/stock/1/liberar-reserva` | `403` | `200` |
| `DELETE /api/stock/1` | `403` | `204` |

---

#### Carrito (ms-carrito) — solo autenticado (el usuario siempre accede a su propio carrito)

| Endpoint | CLIENTE | ADMIN |
|----------|---------|-------|
| `GET /api/carrito/mi-carrito` | `200` (propio) | `200` |
| `POST /api/carrito/items` | `200` | `200` |
| `PUT /api/carrito/items/1` | `200` | `200` |
| `DELETE /api/carrito/items/1` | `200` | `200` |
| `DELETE /api/carrito` | `200` | `200` |

**Ejemplo — Usuario obtiene su propio carrito:**
```http
GET http://localhost:8080/api/carrito/mi-carrito
Authorization: Bearer eyJ...  ← token de cliente2@test.com (idUsuario=2)
```
**Respuesta — `200 OK`:** (carrito propio, se crea automáticamente si no existe)
```json
{
  "idCarrito": 2,
  "idUsuario": 2,
  "items": [],
  "total": 0
}
```

---

#### Pedidos (ms-pedidos) — ADMIN o propietario según endpoint

| Endpoint | CLIENTE (propietario) | CLIENTE (otro usuario) | ADMIN |
|----------|----------------------|----------------------|-------|
| `POST /api/pedidos` (idUsuario desde header) | `201` | `403` | `201` |
| `POST /api/pedidos/{id}/pagar` (pedido propio) | `200` | `403` | `200` |
| `POST /api/pedidos/{id}/confirmar` (pedido propio) | `200` | `403` | `200` |
| `GET /api/pedidos/{id}` (pedido propio) | `200` | `403` | `200` |
| `GET /api/pedidos/mis-pedidos` | `200` (propio) | `403` | `200` |
| `GET /api/pedidos` (listar todos) | `403` | `403` | `200` |
| `PUT /api/pedidos/{id}/estado` | `403` | `403` | `200` |

**Ejemplo — CLIENTE intenta listar todos los pedidos:**
```http
GET http://localhost:8080/api/pedidos
Authorization: Bearer eyJ...  ← token CLIENTE
```
**Respuesta — `403 Forbidden`:**
```json
{
  "status": 403,
  "error": "FORBIDDEN",
  "message": "No tienes permisos de administrador para realizar esta accion"
}
```

---

#### Pagos (ms-pagos) — solo ADMIN

| Endpoint | CLIENTE | ADMIN |
|----------|---------|-------|
| `GET /api/pagos` | `403` | `200` |
| `POST /api/pagos/{id}/procesar` | `403` | `200` |
| `POST /api/pagos/{id}/anular` | `403` | `200` |
| `POST /api/pagos/{id}/reintentar` | `403` | `200` |

---

#### Envíos (ms-envios) — solo ADMIN

| Endpoint | CLIENTE | ADMIN |
|----------|---------|-------|
| `GET /api/envios` | `403` | `200` |
| `GET /api/envios/1` | `403` | `200` |
| `GET /api/envios/pedido/1` | `403` | `200` |
| `GET /api/envios/estado/EN_PREPARACION` | `403` | `200` |
| `PUT /api/envios/1/estado` | `403` | `200` |
| `POST /api/envios/1/cancelar` | `403` | `200` |

---

#### Notificaciones (ms-notificaciones) — ADMIN o propietario

| Endpoint | CLIENTE (propietario) | CLIENTE (otro usuario) | ADMIN |
|----------|----------------------|----------------------|-------|
| `GET /api/notificaciones/mis-notificaciones` | `200` (propio) | `200` (propio) | `200` |
| `GET /api/notificaciones` (todas) | `403` | `403` | `200` |
| `GET /api/notificaciones/1` | `403` | `403` | `200` |
| `DELETE /api/notificaciones/1` | `403` | `403` | `204` |

---

#### Seguridad (security-service) — solo ADMIN (o llamada interna con API Key)

| Endpoint | CLIENTE | ADMIN |
|----------|---------|-------|
| `GET /api/roles` | `403` | `200` |
| `POST /api/roles` | `403` | `201` |
| `PUT /api/roles/1` | `403` | `200` |
| `DELETE /api/roles/1` | `403` | `204` |
| `GET /api/permisos` | `403` | `200` |
| `POST /api/permisos` | `403` | `201` |
| `DELETE /api/permisos/1` | `403` | `204` |
| `POST /api/usuarios-roles` | `403` | `201` |
| `DELETE /api/usuarios-roles` | `403` | `204` |

> **Nota:** Las llamadas internas (Feign desde `user-service` y `auth-service`) usan
> `X-Internal-Api-Key` y no necesitan token JWT.

---

### 2.2 — Resumen rápido: contraste de permisos

Usando el token de **CLIENTE** del paso 1.2:
```http
GET http://localhost:8080/api/pedidos
Authorization: Bearer eyJ...  ← token CLIENTE
```
**Respuesta esperada — `403 Forbidden`.**

Usando el token de **ADMIN** del paso 0.3.3:
```http
GET http://localhost:8080/api/pedidos
Authorization: Bearer eyJ...  ← token ADMIN
```
**Respuesta esperada — `200 OK`** con lista de pedidos.

**Contraste de permisos verificado.**
- CLIENTE → `403 Forbidden`
- ADMIN → `200 OK`

---

## Etapa 3: Pruebas de Errores, Resiliencia e Idempotencia

### 3.1 — Reintento de un pago rechazado

Primero crea un pedido (repitiendo pasos 1.4 a 1.6) que quedará en estado `CREADO`.

Ahora podemos simular un pago rechazado desde `ms-pagos`. Si el procesador interno rechaza el pago (por ejemplo, la lógica de `ms-pagos` rechaza automáticamente algunos pagos para simular fallo), el pedido debe quedar en `CREADO` para reintentar.

**Pagar (podría rechazar):**
```http
POST http://localhost:8080/api/pedidos/2/pagar
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Si el pago falla — respuesta esperada `400 Bad Request` o `500`:**
```json
{
  "error": "Bad Request",
  "message": "El pago fue rechazado. Puedes reintentar..."
}
```

**Reintentar el pago directamente contra ms-pagos:**
```http
POST http://localhost:8080/api/pagos/1/reintentar
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Respuesta esperada — `200 OK`:**
```json
{
  "idTransaccion": 1,
  "idPedido": 2,
  "montoTotal": 1798.00,
  "metodoPago": "TARJETA",
  "estado": "COMPLETADO",
  "referenciaExterna": "...",
  "creadoEn": "2026-06-23T12:00:00",
  "procesadoEn": "2026-06-23T12:00:05"
}
```

> `pagarPedido` ya no funcionara porque el pago ya existe para este pedido. En su lugar, avanza el estado manualmente y luego confirma:

**Avanzar el pedido a PAGADO** (el pago ya esta COMPLETADO):
```http
PUT http://localhost:8080/api/pedidos/2/estado?estado=PAGADO
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Respuesta esperada — `200 OK`:**
```json
{
  "idPedido": 2,
  "idUsuario": 1,
  "estado": "PAGADO",
  ...
}
```

**Confirmar pedido** (reduce stock, crea envio, vacia carrito, notifica):
```http
POST http://localhost:8080/api/pedidos/2/confirmar
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Respuesta esperada — `200 OK`:**
```json
{
  "idPedido": 2,
  "idUsuario": 1,
  "estado": "EN_PREPARACION",
  ...
}
```

> **Alternativa:** Puedes probar la cancelación directa del pago:
> ```http
> POST http://localhost:8080/api/pagos/1/anular
> Authorization: Bearer eyJ...
> ```

---

### 3.2 — Cancelación de pedido y compensación de stock

> **Nota:** El stock se descuenta solo al **confirmar** el pedido (paso 1.8), no al pagar. Por lo tanto:
> - Cancelar desde `PAGADO` (stock aun no reducido) → **NO** restaura stock
> - Cancelar desde `EN_PREPARACION` (stock ya reducido en confirmacion) → **SI** restaura stock via `restaurarStockPedido()`

**Opción A — Cancelar desde PAGADO (sin compensacion de stock):**

El pedido esta en `PAGADO`, el stock nunca se redujo, solo se cancela:

```http
PUT http://localhost:8080/api/pedidos/2/estado?estado=CANCELADO
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Respuesta esperada — `200 OK`:**
```json
{
  "idPedido": 2,
  "estado": "CANCELADO",
  ...
}
```

El stock NO se modifica porque no se habia descontado.

**Opción B — Cancelar desde EN_PREPARACION (con compensacion de stock):**

Primero crear pedido, pagar y confirmar (pasos 1.4 a 1.8). Luego cancelar:

```http
PUT http://localhost:8080/api/pedidos/2/estado?estado=CANCELADO
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Respuesta esperada — `200 OK`:**
```json
{
  "idPedido": 2,
  "estado": "CANCELADO",
  ...
}
```

**Compensacion:** El servicio ejecuta `restaurarStockPedido()` reponiendo el stock via `stockClient.reponerStock()`. Verificalo consultando el stock:

```http
GET http://localhost:8080/api/stock/1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

La `cantidadDisponible` debe haber aumentado en la cantidad que se descontó al confirmar.

---

### 3.3 — Prueba de idempotencia (reducir stock con misma `idempotencyKey`)

El endpoint `PUT /api/stock/{idVariante}/reducir` acepta un campo `idempotencyKey` para evitar descuentos duplicados.

**Primera llamada:**
```http
PUT http://localhost:8080/api/stock/1/reducir
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "cantidad": 2,
  "idempotencyKey": "unique-key-001"
}
```

**Respuesta esperada — `200 OK`:**
```json
{
  "idVariante": 1,
  "cantidadDisponible": 16
}
```

**Segunda llamada (exactamente la misma `idempotencyKey`):**
```http
PUT http://localhost:8080/api/stock/1/reducir
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "cantidad": 2,
  "idempotencyKey": "unique-key-001"
}
```

**Respuesta esperada — `200 OK` (mismo resultado, stock NO se descuenta dos veces):**
```json
{
  "idVariante": 1,
  "cantidadDisponible": 16
}
```

**Idempotencia verificada.** El stock sigue en 16 (no en 14) porque la clave `unique-key-001` ya fue procesada.

---

## Etapa 4: Seguridad y Tokens

### 4.1 — Obtener nuevo JWT usando Refresh Token

Cuando el token actual expire (o para probar el ciclo de vida), usa el `refreshToken` que recibiste en el login (paso 1.2).

```http
POST http://localhost:8080/api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "dGhpcyBpcyBh...refresh..."
}
```

**Respuesta esperada — `200 OK`:**
```json
{
  "token":        "eyJ...nuevo-jwt...",
  "tokenType":    "Bearer",
  "idUsuario":    1,
  "email":        "cliente@test.com",
  "roles":        ["ROLE_CLIENTE"],
  "mensaje":      "Token renovado exitosamente",
  "refreshToken": "nuevo-refresh-token..."
}
```

> **GUARDA el nuevo** `token` — el anterior ya no es valido.
> El endpoint también devuelve un **nuevo `refreshToken`** para futuras renovaciones (rotación de tokens).

---

### Resumen de flujo de tokens

```
Registro (público)
    ↓
Login → obtienes { token, refreshToken }
    ↓
Usas Bearer token para todas las llamadas protegidas
    ↓
Cuando el token expira → POST /api/auth/refresh con refreshToken
    ↓
Obtienes { token (nuevo), refreshToken (nuevo) }
    ↓
Repite el ciclo
```

---

## Etapa 5: Operaciones de Actualización y Eliminación

Pruebas de métodos `PUT` y `DELETE` en los distintos recursos del sistema.

---

### 5.1 — Actualizar perfil de usuario (PUT)

Usando el token de **CLIENTE** (el mismo del login en paso 1.2):

```http
PUT http://localhost:8080/api/usuarios/1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "nombre": "Cliente Actualizado",
  "telefono": "5599998888"
}
```

**Respuesta esperada — `200 OK`:**
```json
{
  "idUsuario": 1,
  "email": "cliente@test.com",
  "nombre": "Cliente Actualizado",
  "telefono": "5599998888",
  "estado": "ACTIVO"
}
```

> Restaura el nombre original para el resto de las pruebas:
> ```http
> PUT http://localhost:8080/api/usuarios/1
> Authorization: Bearer eyJ...
> Content-Type: application/json
>
> { "nombre": "Cliente Test", "telefono": "5512345678" }
> ```

---

### 5.2 — Agregar dirección de envío (POST en recurso anidado)

```http
POST http://localhost:8080/api/usuarios/1/direcciones
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "calle": "Av. Reforma",
  "numero": "123",
  "comuna": "Juárez",
  "ciudad": "CDMX",
  "tipoAlias": "Casa"
}
```

**Respuesta esperada — `201 Created`:**
```json
{
  "idDireccion": 1,
  "calle": "Av. Reforma",
  "numero": "123",
  "comuna": "Juárez",
  "ciudad": "CDMX",
  "tipoAlias": "Casa"
}
```

---

### 5.3 — Actualizar cantidad de item en carrito (PUT)

```http
PUT http://localhost:8080/api/carrito/items/1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "cantidad": 5
}
```

**Respuesta esperada — `200 OK`:**
```json
{
  "idCarrito": 1,
  "idUsuario": 1,
  "items": [
    {
      "idItem": 1,
      "idVariante": 1,
      "cantidad": 5,
      "precioUnitario": 899.00,
      "subtotal": 4495.00
    }
  ],
  "total": 4495.00
}
```

> Restaura a 2 para continuar las pruebas:
> ```http
> PUT http://localhost:8080/api/carrito/items/1
> Authorization: Bearer eyJ...
> Content-Type: application/json
>
> { "cantidad": 2 }
> ```

---

### 5.4 — Eliminar item del carrito (DELETE)

```http
DELETE http://localhost:8080/api/carrito/items/1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

**Respuesta esperada — `200 OK`:** (devuelve el carrito actualizado)
```json
{
  "idCarrito": 1,
  "idUsuario": 1,
  "items": [],
  "total": 0
}
```

> **IMPORTANTE:** Despues de esta prueba, vuelve a agregar el item (paso 1.5) antes de continuar con el flujo de pedidos.

---

### 5.5 — Actualizar perfume (PUT — requiere ADMIN)

Usando el token de **ADMIN**:

```http
PUT http://localhost:8080/api/catalogo/perfumes/1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...  ← token ADMIN
Content-Type: application/json

{
  "idCategoria": 1,
  "idMarca": 1,
  "concentracion": "EDT",
  "nombre": "Bleu de Prueba (Actualizado)",
  "descripcion": "Descripción actualizada del perfume"
}
```

**Respuesta esperada — `200 OK`:**
```json
{
  "idPerfume": 1,
  "nombre": "Bleu de Prueba (Actualizado)",
  "concentracion": "EDT",
  "descripcion": "Descripción actualizada del perfume",
  "estado": "ACTIVO",
  "idMarca": 1,
  "nombreMarca": "Chanel",
  "idCategoria": 1,
  "nombreCategoria": "Masculino",
  "variantes": [
    { "idVariante": 1, "idPerfume": 1, "sku": "BLEU-50", "ml": 50, "precio": 899.00 },
    { "idVariante": 2, "idPerfume": 1, "sku": "BLEU-100", "ml": 100, "precio": 1499.00 }
  ]
}
```

> Restaura el nombre original:
> ```http
> PUT http://localhost:8080/api/catalogo/perfumes/1
> Authorization: Bearer eyJ...  ← token ADMIN
> Content-Type: application/json
>
> { "idCategoria": 1, "idMarca": 1, "concentracion": "EDP", "nombre": "Bleu de Prueba", "descripcion": "Perfume de prueba para E2E" }
> ```

---

### 5.6 — Eliminar variante (DELETE — requiere ADMIN)

```http
DELETE http://localhost:8080/api/catalogo/variantes/3
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...  ← token ADMIN
```

**Respuesta esperada — `204 No Content`** (sin cuerpo de respuesta).

Verifica que la variante fue eliminada consultando el perfume:
```http
GET http://localhost:8080/api/catalogo/perfumes/2
```

**Respuesta esperada — `200 OK`** (variante 3 ya no aparece):
```json
{
  "idPerfume": 2,
  "nombre": "J'adore Test",
  "variantes": []
}
```

> **NOTA:** Si necesitas la variante para otras pruebas, creala de nuevo con el paso 0.4.

---

### 5.7 — Eliminar perfil de usuario (DELETE — solo ADMIN)

Con token de **ADMIN**, elimina (desactiva lógicamente) al usuario cliente:
```http
DELETE http://localhost:8080/api/usuarios/1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...  ← token ADMIN
```

**Respuesta esperada — `204 No Content`** (baja lógica, sin contenido en la respuesta).

> **NOTA:** Si vas a repetir el flujo completo, necesitarás registrar un nuevo usuario o reactivar este en BD.

---

## Checklist rápido de smoke test

| # | Prueba | Método | Endpoint | Token | Código esperado |
|---|--------|--------|----------|-------|----------------|
| 1 | Poblar: ver marcas | `GET` | `/api/catalogo/marcas` | — | `200` |
| 2 | Poblar: crear perfume | `POST` | `/api/catalogo/perfumes` | ADMIN | `201` |
| 3 | Poblar: crear variante | `POST` | `/api/catalogo/perfumes/{id}/variantes` | ADMIN | `201` |
| 4 | Poblar: crear inventario | `POST` | `/api/stock/{idVariante}` | ADMIN | `201` |
| 5 | Poblar: reponer stock | `PUT` | `/api/stock/{id}/reponer` | ADMIN | `200` |
| 6 | Registro | `POST` | `/api/usuarios` | — | `201` |
| 7 | Login | `POST` | `/api/auth/login` | — | `200` + token |
| 8 | Catálogo público | `GET` | `/api/catalogo/productos` | — | `200` |
| 9 | Agregar item carrito | `POST` | `/api/carrito/items` | CLIENTE | `200` |
| 10 | Ver mi carrito | `GET` | `/api/carrito/mi-carrito` | CLIENTE | `200` |
| 11 | Actualizar item carrito | `PUT` | `/api/carrito/items/{idItem}` | CLIENTE | `200` |
| 12 | Eliminar item carrito | `DELETE` | `/api/carrito/items/{idItem}` | CLIENTE | `200` |
| 13 | Crear pedido (propio) | `POST` | `/api/pedidos` | CLIENTE | `201` |
| 14 | Crear pedido (otro user) | `POST` | `/api/pedidos` | OTRO USER | `403` |
| 15 | Pagar pedido | `POST` | `/api/pedidos/{id}/pagar` | CLIENTE | `200` → `PAGADO` |
| 16 | Confirmar pedido | `POST` | `/api/pedidos/{id}/confirmar` | CLIENTE | `200` → `EN_PREPARACION` |
| 17 | RBAC: catálogo write como CLIENTE | `POST` | `/api/catalogo/perfumes` | CLIENTE | `403` |
| 18 | RBAC: stock write como CLIENTE | `PUT` | `/api/stock/1/reponer` | CLIENTE | `403` |
| 19 | RBAC: listar pedidos como CLIENTE | `GET` | `/api/pedidos` | CLIENTE | `403` |
| 20 | RBAC: cambiar estado pedido como CLIENTE | `PUT` | `/api/pedidos/1/estado` | CLIENTE | `403` |
| 21 | RBAC: pagos como CLIENTE | `GET` | `/api/pagos` | CLIENTE | `403` |
| 22 | RBAC: envios como CLIENTE | `GET` | `/api/envios` | CLIENTE | `403` |
| 23 | RBAC: seguridad como CLIENTE | `GET` | `/api/roles` | CLIENTE | `403` |
| 24 | RBAC: admin en ruta admin | `PUT` | `/api/pedidos/1/estado` | ADMIN | `200` |
| 25 | Ownership: pedido ajeno | `GET` | `/api/pedidos/1` | OTRO USER | `403` |
| 26 | Ver carrito propio | `GET` | `/api/carrito/mi-carrito` | CLIENTE | `200` |
| 27 | Ver notificaciones propias | `GET` | `/api/notificaciones/mis-notificaciones` | CLIENTE | `200` |
| 28 | Reintentar pago (admin) | `POST` | `/api/pagos/{id}/reintentar` | ADMIN | `200` |
| 29 | Idempotencia (2 veces) | `PUT` | `/api/stock/{id}/reducir` | ADMIN | `200` (sin cambios 2ª vez) |
| 30 | Actualizar perfil usuario | `PUT` | `/api/usuarios/{id}` | CLIENTE | `200` |
| 31 | Agregar dirección | `POST` | `/api/usuarios/{id}/direcciones` | CLIENTE | `201` |
| 32 | Actualizar perfume | `PUT` | `/api/catalogo/perfumes/{id}` | ADMIN | `200` |
| 33 | Eliminar variante | `DELETE` | `/api/catalogo/variantes/{id}` | ADMIN | `204` |
| 34 | Eliminar usuario (admin) | `DELETE` | `/api/usuarios/{id}` | ADMIN | `204` |
| 35 | Refresh token | `POST` | `/api/auth/refresh` | — | `200` |
