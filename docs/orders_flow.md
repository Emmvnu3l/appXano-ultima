# Flujo de Órdenes, Base de Datos y Permisos

## Flujo de creación de órdenes

1. El usuario inicia checkout y se calcula el total a partir de productos del carrito.
2. Se construye el payload `CreateOrderRequest` con `items`, `total`, `status="pendiente"` y `user_id` del usuario autenticado.
3. Se envía `POST /order` a la API de Xano. Si el contrato exige campos diferentes, se usa un fallback crudo incluyendo `user_id`.
4. Tras la confirmación, se limpia el carrito y se actualiza stock de productos.

## Estructura de la base de datos

- Tabla `order` con campos: `id`, `created_at`, `total`, `status`, `user_id`, `discount_code_id`.
- `user_id` debe ser entero y relacionar la orden con el usuario creador; validar que no sea nulo/0.

## Permisos y filtros

- Usuarios no admin:
  - Listan órdenes con filtro `user_id` (servidor) y adicionalmente se filtra en cliente por `user_id` y estados válidos.
  - Estados válidos: `pendiente`, `confirmada`, `en_proceso`, `enviado`, `aceptado`, `completada`.
- Admin:
  - Pueden listar todas las órdenes, cambiar estados y ver órdenes canceladas/rechazadas.

## Consultas

- Listado: `GET /order?user_id=<ID>&status=<opcional>&from=<opcional>&to=<opcional>`.
- Ordenación: se realiza en cliente por `created_at` descendente.

## Pruebas

- Unitarias (JVM) para filtros de `user_id`, estados válidos y paginación.