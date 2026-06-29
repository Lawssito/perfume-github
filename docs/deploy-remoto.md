# Deploy Remoto - Perfume Platform

## Requisitos

- Cuenta en [Railway.app](https://railway.app) o [Render.com](https://render.com)
- Docker instalado localmente (para pruebas)
- Git
- Repositorio del proyecto subido a GitHub

## Proveedores de base de datos

Ambos proveedores ofrecen MySQL o bases de datos relacionales:
- **Railway**: MySQL nativo como servicio (más simple)
- **Render**: PostgreSQL (necesitarás ajustar `spring.datasource.driver-class-name` y dialect de Hibernate)

Este documento asume Railway como opción principal.

---

## Estrategia de despliegue

El orden de despliegue es crítico por la dependencia de Eureka:

1. **MySQL** (Railway)
2. **eureka-server**
3. **security-service**
4. **auth-service**
5. **user-service**
6. **ms-catalogo**, **ms-stock**, **ms-pagos**, **ms-notificaciones**
7. **ms-carrito**, **ms-envios**
8. **ms-pedidos**
9. **api-gateway**

Cada microservicio se despliega con su `Dockerfile` existente. No se necesita Docker Compose en el entorno remoto.

---

## Variables de entorno comunes

Todas las aplicaciones Spring Boot comparten estas variables:

| Variable | Descripción |
|---|---|
| `SPRING_PROFILES_ACTIVE=prod` | Activa el perfil de producción |
| `SPRING_DATASOURCE_URL=jdbc:mysql://...` | URL de la base de datos |
| `SPRING_DATASOURCE_USERNAME=...` | Usuario de la base de datos |
| `SPRING_DATASOURCE_PASSWORD=...` | Contraseña de la base de datos |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/` | URL de Eureka (interno Railway) |
| `SPRING_CLOUD_CONFIG_ENABLED=false` | Deshabilitar config server si no se usa |

Para **Eureka Server** adicional:
| `EUREKA_CLIENT_REGISTERWITHEUREKA=false` |
| `EUREKA_CLIENT_FETCHREGISTRY=false` |

---

## Pasos para Railway

### 1. Crear proyecto en Railway

1. Inicia sesión en [Railway.app](https://railway.app)
2. Crea un **New Project** → **Empty Project**
3. Conecta tu repositorio de GitHub desde **Deploy** → **GitHub Repo**
4. Railway detectará automáticamente los Dockerfiles

### 2. Desplegar MySQL

1. Dentro del proyecto Railway, haz clic en **New** → **Database** → **Add MySQL**
2. Espera a que se provisione
3. Ve a la pestaña **Connect** y copia la `MYSQL_URL`, `MYSQL_USER` y `MYSQL_ROOT_PASSWORD`

La URL típica de Railway MySQL se ve así:
```
mysql://root:password@mysql.internal:3306/railway
```
Conviértela al formato JDBC:
```
jdbc:mysql://mysql.internal:3306/railway?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
```

### 3. Desplegar eureka-server

1. Crea un nuevo servicio desde **New** → **GitHub Repo** → selecciona el repo y apunta al subdirectorio `eureka-server`
2. Railway detecta el `Dockerfile` automáticamente
3. Agrega variables de entorno:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `EUREKA_CLIENT_REGISTERWITHEUREKA=false`
   - `EUREKA_CLIENT_FETCHREGISTRY=false`
4. Railway asigna un dominio interno como `eureka-server.up.railway.app`
5. Anota la URL interna para usarla en los demás servicios

### 4. Desplegar security-service

1. Nuevo servicio → mismo repo → subdirectorio `security-service`
2. Variables de entorno:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server.up.railway.app:80/eureka/`
   - Las variables de datasource de su base (cada microservicio con su propia BD o una compartida)
3. Espera a que el build termine

> **Importante**: En Railway, el puerto 80 redirige al puerto expuesto por el contenedor. Usa `:80` en las URLs de Eureka para servicios dentro del mismo proyecto.

### 5. Desplegar el resto de microservicios

Repite el paso anterior para cada módulo:

- `auth-service`
- `user-service`
- `ms-catalogo`
- `ms-stock`
- `ms-pagos`
- `ms-notificaciones`
- `ms-carrito`
- `ms-envios`
- `ms-pedidos`
- `api-gateway`

Variables comunes para cada uno:
- `SPRING_PROFILES_ACTIVE=prod`
- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server.up.railway.app:80/eureka/`
- `SPRING_DATASOURCE_URL=jdbc:mysql://...`
- `SPRING_DATASOURCE_USERNAME=root`
- `SPRING_DATASOURCE_PASSWORD=...`

**api-gateway** adicionalmente necesita:
- `SERVER_PORT=80` (Railway asigna puertos dinámicamente, pero Spring Gateway necesita saber en qué puerto escuchar)

### 6. Verificar el despliegue

1. Abre el dominio público del `api-gateway` (Railway asigna un `<proyecto>.up.railway.app`)
2. Prueba endpoints públicos como `/api/auth/login`
3. Revisa los logs de cada servicio en Railway Dashboard

---

## Pasos para Render

### 1. Crear servicio MySQL

1. En [Render.com](https://render.com), ve a **Dashboard** → **New** → **PostgreSQL**
2. Configura nombre, usuario y contraseña
3. Anota la **Internal Database URL** (formato `postgres://...`)

Render no ofrece MySQL nativo. Necesitas:

- Opción A: Usar un servicio externo como [Aiven MySQL](https://aiven.io/mysql) o [PlanetScale](https://planetscale.com)
- Opción B: Migrar a PostgreSQL y ajustar:
  - `pom.xml`: cambiar `mysql-connector-j` por `org.postgresql:postgresql`
  - `application-prod.yml`: cambiar `spring.datasource.driver-class-name=org.postgresql.Driver` y dialect a `org.hibernate.dialect.PostgreSQLDialect`

### 2. Desplegar servicios con Render Blueprint (recomendado)

Render permite usar un `render.yaml` en la raíz del repo para desplegar múltiples servicios. Alternativamente, despliega manualmente:

1. **New Web Service** → **Connect GitHub repo**
2. Configura:
   - **Name**: `eureka-server`
   - **Root Directory**: `eureka-server`
   - **Build Command**: `./mvnw clean package -DskipTests` (o usa Docker)
   - **Start Command**: `java -jar target/*.jar`
3. Agrega las mismas variables de entorno listadas arriba
4. Repite para cada módulo

### 3. Diferencia clave: Docker en Render

Render puede usar Docker si seleccionas **Runtime** → **Docker** al crear el servicio. Si usas Docker, Render usará el `Dockerfile` de cada subdirectorio automáticamente.

Si usas **Native Java** (no Docker), necesitarás:
- Build Command: `cd ms-pedidos && ./mvnw clean package -DskipTests`
- Start Command: `java -jar ms-pedidos/target/*.jar`

---

## Notas importantes

### Red interna vs. pública

- **Railway**: Los servicios dentro del mismo proyecto se comunican por el dominio `.up.railway.app:80` incluso sin plan pago
- **Render**: Los servicios dentro de la misma organización pueden usar `.onrender.com` y el plan gratuito tiene limitaciones de ancho de banda

### Health checks

Railway y Render monitorean los endpoints `/actuator/health`. Asegúrate de tener `spring-boot-starter-actuator` en los microservicios que se despliegan.

### Logs y debugging

- Railway: Logs en tiempo real desde el dashboard
- Render: Similar, con opción de exportar

### Costos

- Railway: Plan gratuito limitado a $5 de crédito/mes, suficiente para pruebas ligeras
- Render: Plan gratuito con 750 horas/mes por servicio (se duerme tras 15 min de inactividad)

---

## Comandos útiles (local)

```bash
# Construir imagen Docker de un módulo
docker build -t ms-pedidos ./ms-pedidos

# Probar localmente con Docker Compose (desarrollo)
docker-compose up -d

# Ver logs de todos los servicios
docker-compose logs -f
```
