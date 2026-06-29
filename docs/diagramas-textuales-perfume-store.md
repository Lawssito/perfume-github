# Diagramas textuales del proyecto Perfume Store API

Este documento contiene tres diagramas explicados en formato Markdown, sin imágenes ni archivos externos.  
El objetivo es que la arquitectura sea entendible directamente desde GitHub o desde el README.

---

# 1. Diagrama de arquitectura de microservicios

## Descripción general

El sistema está organizado bajo una arquitectura de microservicios.  
El usuario o cliente externo no accede directamente a cada microservicio, sino que entra al sistema mediante el API Gateway.

El API Gateway centraliza las solicitudes y las redirige hacia el microservicio correspondiente.  
Eureka Server permite que los servicios se registren y puedan ser descubiertos dentro del ecosistema.

## Componentes principales

| Componente | Rol dentro del sistema |
|---|---|
| Cliente / Frontend / Postman | Punto desde donde se realizan las solicitudes HTTP al sistema. |
| API Gateway | Entrada única al sistema. Redirige solicitudes hacia los microservicios. |
| Eureka Server | Servidor de descubrimiento donde se registran los microservicios. |
| auth-service | Gestiona autenticación y generación/validación de acceso. |
| security-service | Apoya la seguridad del sistema y validaciones de autorización. |
| user-service | Gestiona usuarios del sistema. |
| ms-catalogo | Gestiona productos o perfumes disponibles. |
| ms-stock | Gestiona disponibilidad y stock de productos. |
| ms-carrito | Gestiona el carrito de compra del usuario. |
| ms-pedidos | Gestiona la creación y seguimiento de pedidos. |
| ms-pagos | Gestiona procesos asociados a pagos. |
| ms-envios | Gestiona información y seguimiento de envíos. |
| ms-notificaciones | Gestiona avisos o notificaciones del sistema. |

## Diagrama textual

```text
                         +-----------------------------+
                         | Cliente / Frontend / Postman |
                         +---------------+-------------+
                                         |
                                         v
                                +----------------+
                                |  API Gateway   |
                                +--------+-------+
                                         |
              ----------------------------------------------------
              |           |           |           |              |
              v           v           v           v              v
      +-------------+ +------------+ +------------+ +-------------+
      | auth-service| |user-service| |security-s. | |ms-catalogo  |
      +-------------+ +------------+ +------------+ +-------------+

              |           |           |           |              |
              v           v           v           v              v
      +-------------+ +------------+ +------------+ +-------------+
      |  ms-stock   | |ms-carrito | |ms-pedidos | |  ms-pagos   |
      +-------------+ +------------+ +------------+ +-------------+

              |                         |
              v                         v
      +-------------+           +------------------+
      |  ms-envios  |           |ms-notificaciones |
      +-------------+           +------------------+

Todos los microservicios se registran en Eureka Server.

                                +----------------+
                                | Eureka Server  |
                                +----------------+
```

## Lectura del diagrama

1. El cliente envía una solicitud HTTP al API Gateway.
2. El API Gateway consulta los servicios disponibles registrados en Eureka.
3. El Gateway redirige la solicitud al microservicio correspondiente.
4. Cada microservicio cumple una responsabilidad específica.
5. Los microservicios pueden comunicarse entre sí cuando el flujo de negocio lo requiere.

---

# 2. Diagrama de comunicación con Eureka y API Gateway

## Descripción general

Este diagrama explica cómo se comunican los componentes principales del sistema.

Eureka Server funciona como registro de servicios.  
Cada microservicio se registra en Eureka al iniciar.  
El API Gateway utiliza esa información para enrutar solicitudes mediante los nombres de los servicios.

## Flujo principal de comunicación

| Paso | Acción |
|---|---|
| 1 | Cada microservicio inicia y se registra en Eureka Server. |
| 2 | El cliente realiza una solicitud HTTP al API Gateway. |
| 3 | El API Gateway consulta Eureka para ubicar el microservicio requerido. |
| 4 | El Gateway redirige la solicitud al microservicio correspondiente. |
| 5 | El microservicio procesa la solicitud y devuelve una respuesta. |
| 6 | El API Gateway devuelve la respuesta al cliente. |

## Diagrama textual

```text
[1] Registro de servicios

+-------------------+        se registra en        +----------------+
| auth-service      | ---------------------------> |                |
| user-service      | ---------------------------> |                |
| security-service  | ---------------------------> | Eureka Server  |
| ms-catalogo       | ---------------------------> |                |
| ms-stock          | ---------------------------> |                |
| ms-carrito        | ---------------------------> |                |
| ms-pedidos        | ---------------------------> |                |
| ms-pagos          | ---------------------------> |                |
| ms-envios         | ---------------------------> |                |
| ms-notificaciones | ---------------------------> |                |
+-------------------+                              +----------------+


[2] Solicitud desde el cliente

+-----------------------------+
| Cliente / Frontend / Postman|
+--------------+--------------+
               |
               | HTTP Request
               v
+--------------+--------------+
|          API Gateway         |
+--------------+--------------+
               |
               | consulta servicios registrados
               v
+--------------+--------------+
|          Eureka Server       |
+--------------+--------------+
               |
               | devuelve ubicación lógica del servicio
               v
+--------------+--------------+
|          API Gateway         |
+--------------+--------------+
               |
               | enruta solicitud
               v
+-----------------------------+
| Microservicio correspondiente|
+-----------------------------+
               |
               | respuesta HTTP
               v
+-----------------------------+
| Cliente / Frontend / Postman|
+-----------------------------+
```

## Comunicación interna entre microservicios

Algunos microservicios no trabajan completamente aislados.  
En ciertos casos necesitan consultar o activar procesos en otros servicios.

```text
auth-service
    |
    | consulta información de usuario
    v
user-service


security-service
    |
    | valida autenticación o permisos
    v
auth-service


ms-carrito
    |
    | consulta productos
    v
ms-catalogo


ms-carrito
    |
    | consulta disponibilidad
    v
ms-stock


ms-pedidos
    |
    | valida stock
    v
ms-stock


ms-pedidos
    |
    | solicita procesamiento de pago
    v
ms-pagos


ms-pedidos
    |
    | solicita gestión de envío
    v
ms-envios


ms-pedidos
    |
    | genera aviso al usuario
    v
ms-notificaciones
```

## Lectura del diagrama

El API Gateway evita que el cliente tenga que conocer la ubicación real de cada microservicio.  
Eureka permite que el sistema sea más flexible, porque los servicios se localizan por nombre y no por una dirección fija.

---

# 3. Diagrama de base de datos por microservicio

## Descripción general

En una arquitectura de microservicios, cada microservicio puede tener su propia base de datos.  
Esto permite separar responsabilidades, evitar acoplamiento excesivo y mantener independencia entre servicios.

En este proyecto, los microservicios de negocio poseen persistencia propia según su responsabilidad.  
El `security-service` puede funcionar como apoyo de seguridad y no necesariamente requiere una base de datos propia si delega información en `auth-service` o `user-service`.

## Relación microservicio y base de datos

| Microservicio | Base de datos asociada | Responsabilidad principal |
|---|---|---|
| auth-service | db_auth | Datos de autenticación, credenciales o acceso. |
| user-service | db_user | Datos de usuarios del sistema. |
| security-service | Sin BD propia o apoyo a auth/user | Validaciones de seguridad y autorización. |
| ms-catalogo | db_catalogo | Productos, perfumes, precios y descripción. |
| ms-stock | db_stock | Disponibilidad y cantidad de productos. |
| ms-carrito | db_carrito | Productos agregados al carrito. |
| ms-pedidos | db_pedidos | Órdenes o pedidos generados. |
| ms-pagos | db_pagos | Información del pago y estado de transacciones. |
| ms-envios | db_envios | Información de despacho o seguimiento. |
| ms-notificaciones | db_notificaciones | Registro o envío de notificaciones. |

## Diagrama textual

```text
+----------------+        +----------------+
| auth-service   | -----> | db_auth        |
+----------------+        +----------------+

+----------------+        +----------------+
| user-service   | -----> | db_user        |
+----------------+        +----------------+

+-------------------+
| security-service  |
+-------------------+
        |
        | puede apoyarse en auth-service o user-service
        v
+----------------+        +----------------+
| auth/user      | -----> | sus BD propias |
+----------------+        +----------------+


+----------------+        +----------------+
| ms-catalogo    | -----> | db_catalogo    |
+----------------+        +----------------+

+----------------+        +----------------+
| ms-stock       | -----> | db_stock       |
+----------------+        +----------------+

+----------------+        +----------------+
| ms-carrito     | -----> | db_carrito     |
+----------------+        +----------------+

+----------------+        +----------------+
| ms-pedidos     | -----> | db_pedidos     |
+----------------+        +----------------+

+----------------+        +----------------+
| ms-pagos       | -----> | db_pagos       |
+----------------+        +----------------+

+----------------+        +----------------+
| ms-envios      | -----> | db_envios      |
+----------------+        +----------------+

+---------------------+        +---------------------+
| ms-notificaciones   | -----> | db_notificaciones   |
+---------------------+        +---------------------+
```

## Lectura del diagrama

1. Cada microservicio administra su propio dominio.
2. Cada base de datos almacena sólo la información que corresponde a ese microservicio.
3. No se recomienda que un microservicio modifique directamente la base de datos de otro.
4. Cuando un servicio necesita información de otro, debe comunicarse por API REST o Feign Client.
5. Esta separación mejora la mantenibilidad y permite desplegar servicios de manera más independiente.

---

# Resumen de arquitectura

```text
Cliente
   |
   v
API Gateway
   |
   v
Microservicios registrados en Eureka
   |
   v
Base de datos propia por microservicio
```

El sistema queda organizado en capas distribuidas, con entrada centralizada por Gateway, descubrimiento mediante Eureka y persistencia separada por responsabilidad.
