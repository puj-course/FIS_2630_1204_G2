# Gastroflow - HU-35 adaptada al DDL completo

Proyecto Maven para IntelliJ + JavaFX + PostgreSQL, conectado al esquema `Gastroflow_Completo_HU35.ddl` incluido en `database/`.

## Tablas utilizadas

### pedidos
La HU-35 usa directamente:
- `pedido_id`
- `numero_pedido`
- `mesa_id`
- `subtotal`
- `descuento`
- `impuestos`
- `propina`
- `total`
- `estado`
- `fecha_actualizacion`

### pagos
Al cerrar una cuenta registra:
- `pago_id`
- `pedido_id`
- `usuario_id`
- `subtotal`
- `descuento`
- `impuestos`
- `propina`
- `motivo_descuento`
- `total_pagado`
- `estado = 'PAGADO'`
- `fecha_pago`

### usuarios y roles
Antes de guardar el pago, el DAO comprueba que `usuario_id`:
- exista en `usuarios`;
- esté activo (`is_active = 1`);
- tenga en `roles.nombre_rol` el valor `CAJERO`.

### mesas
La pantalla muestra `mesas.numero_mesa`, no el identificador interno `id_mesa`.

## Funcionalidad

- Buscar una cuenta por `pedidos.numero_pedido`.
- No permitir pagar pedidos `CANCELADO`.
- Mostrar número de mesa, subtotal e impuestos.
- Agregar, modificar o eliminar descuento.
- Exigir motivo si `descuento > 0`.
- Agregar, modificar o eliminar propina.
- Recalcular automáticamente:

  `total = subtotal + impuestos + propina - descuento`

- Mostrar todos los valores antes del pago.
- Pedir confirmación al Cajero.
- Actualizar `pedidos.descuento`, `pedidos.propina` y `pedidos.total`.
- Crear el registro en `pagos`.
- Bloquear una cuenta que ya tenga un pago `PAGADO`.
- Realizar actualización + pago dentro de una transacción.

## 1. Base de datos

Si vas a crear la BD desde cero, ejecuta:

`database/Gastroflow_Completo_HU35.ddl`

Si ya tienes las tablas creadas, no vuelvas a ejecutar el DDL completo sobre la misma base sin preparar una migración.

## 2. Conexión PostgreSQL

Archivo:

`src/main/java/com/gastroflow/database/ConexionBD.java`

Por defecto usa:

- URL: `jdbc:postgresql://localhost:5432/gastroflow`
- usuario: `postgres`
- contraseña: `postgres`

Puedes cambiar estos valores o usar las variables de entorno:

- `GASTROFLOW_DB_URL`
- `GASTROFLOW_DB_USER`
- `GASTROFLOW_DB_PASSWORD`

## 3. Sesión del Cajero

Cuando tu HU de inicio de sesión esté conectada, llama:

```java
SesionUsuario.iniciarSesion(
    usuario.getIdUsuario(),
    usuario.getNombre(),
    rol.getNombreRol()
);
```

El `id_usuario` debe corresponder a un usuario real con rol `CAJERO`.

### Prueba independiente en IntelliJ

En Run > Edit Configurations > VM options:

```text
-Dgastroflow.cajeroId=1 -Dgastroflow.cajeroNombre="Cajero prueba"
```

El ID debe existir en `usuarios` y estar relacionado con un registro de `roles` cuyo `nombre_rol` sea `CAJERO`.

## 4. Ejecutar

Desde IntelliJ ejecuta `Main.java`.

O con Maven:

```bash
mvn javafx:run
```
