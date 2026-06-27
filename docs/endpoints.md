# API Endpoints - Perfume Platform

## api-gateway (Puerto: 8080)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| ALL | `/api/auth/**` | Redirige a auth-service |
| ALL | `/api/usuarios/**` | Redirige a user-service |
| ALL | `/api/carrito/**` | Redirige a ms-carrito |
| ALL | `/api/catalogo/**` | Redirige a ms-catalogo |
| ALL | `/api/pedidos/**` | Redirige a ms-pedidos |
| ALL | `/api/pagos/**` | Redirige a ms-pagos |
| ALL | `/api/envios/**` | Redirige a ms-envios |
| ALL | `/api/stock/**` | Redirige a ms-stock |
| ALL | `/api/notificaciones/**` | Redirige a ms-notificaciones |
| ALL | `/api/seguridad/**` | Redirige a security-service |

---

## eureka-server (Puerto: 8761)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/` | Panel web de Eureka |

---

## auth-service (Puerto: 8081)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/auth/login` | Inicio de sesión |
| POST | `/api/auth/register` | Registro de usuario |
| POST | `/api/auth/refresh` | Refrescar token JWT |
| POST | `/api/auth/logout` | Cerrar sesión |

---

## user-service (Puerto: 8082)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/usuarios` | Listar usuarios |
| GET | `/api/usuarios/{id}` | Obtener usuario por ID |
| POST | `/api/usuarios` | Crear usuario |
| PUT | `/api/usuarios/{id}` | Actualizar usuario |
| DELETE | `/api/usuarios/{id}` | Eliminar usuario |

---

## security-service (Puerto: 8083)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/seguridad/validar` | Validar token JWT |
| GET | `/api/seguridad/roles/{token}` | Obtener roles del token |

---

## ms-catalogo (Puerto: 8084)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/catalogo/productos` | Listar productos |
| GET | `/api/catalogo/productos/{id}` | Obtener producto |
| POST | `/api/catalogo/productos` | Crear producto |
| PUT | `/api/catalogo/productos/{id}` | Actualizar producto |
| DELETE | `/api/catalogo/productos/{id}` | Eliminar producto (lógico) |
| GET | `/api/catalogo/categorias` | Listar categorías |
| POST | `/api/catalogo/categorias` | Crear categoría |

---

## ms-carrito (Puerto: 8085)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/carrito/{usuarioId}` | Ver carrito |
| POST | `/api/carrito/{usuarioId}/items` | Agregar item al carrito |
| PUT | `/api/carrito/{usuarioId}/items/{itemId}` | Actualizar cantidad |
| DELETE | `/api/carrito/{usuarioId}/items/{itemId}` | Eliminar item del carrito |
| DELETE | `/api/carrito/{usuarioId}` | Vaciar carrito |

---

## ms-pedidos (Puerto: 8086)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/pedidos` | Listar pedidos |
| GET | `/api/pedidos/{id}` | Obtener pedido |
| POST | `/api/pedidos` | Crear pedido desde carrito |
| PUT | `/api/pedidos/{id}/estado` | Actualizar estado del pedido |

---

## ms-pagos (Puerto: 8087)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/pagos` | Procesar pago |
| GET | `/api/pagos/{pedidoId}` | Obtener pago por pedido |

---

## ms-envios (Puerto: 8088)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/envios` | Crear envío |
| GET | `/api/envios/{pedidoId}` | Obtener estado de envío |
| PUT | `/api/envios/{id}/estado` | Actualizar estado de envío |

---

## ms-stock (Puerto: 8089)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/stock/{productoId}` | Ver stock de producto |
| PUT | `/api/stock/{productoId}` | Actualizar stock |
| POST | `/api/stock/validar` | Validar disponibilidad |

---

## ms-notificaciones (Puerto: 8090)
| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | `/api/notificaciones/{usuarioId}` | Listar notificaciones |
| POST | `/api/notificaciones` | Enviar notificación |
| PUT | `/api/notificaciones/{id}/leer` | Marcar como leída |
