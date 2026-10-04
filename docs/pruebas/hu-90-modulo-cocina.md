# HU-XXX · Pruebas funcionales del módulo de cocina (pedidos)

Documento de casos de prueba y resultados.

| | |
|---|---|
| Módulo | Pedidos (`service.PedidoService`, `repository.PedidoRepository`, tabla `pedidos`) |
| Pruebas | `src/test/java/repository/PedidoModuloFuncionalTest.java` |
| Base usada | PostgreSQL 18, base creada desde cero con los DDL y migraciones del repositorio |
| Resultado | 13 casos ejecutados, 13 pasan. Se encontraron y corrigieron 2 defectos |

## Cómo ejecutarlas

```bash
mvn test -Dtest=PedidoModuloFuncionalTest
```

Son pruebas de integración: necesitan una base PostgreSQL accesible con la
conexión configurada en `ConexionDB.ConexionBD`. Parte de las reglas del módulo
(el tipo JSONB de `productos`, las llaves foráneas, el catálogo de estados de
mesa) vive en el SQL y no en el código Java, así que probarlas con objetos
simulados no demostraría nada.

Si no hay base disponible, la clase entera se omite en lugar de fallar.

Las pruebas crean sus propios datos: una zona `ZONA PRUEBAS HU-XXX` y dos mesas
con números 8101 y 8102. Al terminar borran esas mesas, sus pedidos y su
historial de estados. No tocan las mesas reales. Se reutiliza el primer usuario
que exista en `usuarios`, porque `pedidos.usuario_id` es obligatorio.

## Alcance

La HU habla del módulo de cocina. En el código actual no existe una pantalla ni
una clase llamada "cocina": el registro, la consulta y la actualización de
pedidos están en `PedidoService` y `PedidoRepository`, y esas son las clases
probadas. La pantalla "Mesero — Menú y pedidos" solo verifica la disponibilidad
de los productos y todavía no registra pedidos, por lo que no se prueba aquí.

## Casos de prueba

### Registro

**CP-01 · Se registra un pedido PENDIENTE para una mesa libre**
`crearPedido` sobre una mesa libre. Se verifica que el pedido recibe id, que su
estado es `PENDIENTE`, que el número empieza por `PED-` y que el estado guardado
en la base es `PENDIENTE`.
Resultado: **pasa** (después de la corrección 1).

**CP-02 · Al registrar el pedido la mesa pasa a OCUPADA**
Resultado: **pasa**.

**CP-03 · El cambio de estado de la mesa queda en el historial**
Debe existir un registro `APERTURA_PEDIDO` en `historial_estado_mesa`.
Resultado: **pasa**.

### Consulta

**CP-04 · Lo que devuelve `findById` coincide con la base**
Se compara el pedido consultado con un `SELECT` directo: número, mesa, usuario y
estado.
Resultado: **pasa**.

**CP-05 · Consultar un pedido que no existe devuelve vacío**
Resultado: **pasa**.

### Regla de comanda activa

**CP-06 · Una mesa con comanda activa rechaza otro pedido**
Se lanza `MesaOcupadaException` y no se crea un segundo pedido.
Resultado: **pasa**.

### Actualización

**CP-07 · Al cerrar el pedido queda COMPLETADO**
Se verifica el estado devuelto y el guardado en la base.
Resultado: **pasa**.

**CP-08 · Al cerrar el pedido la mesa vuelve a LIBRE**
Además debe quedar un registro `CIERRE_PEDIDO` en el historial.
Resultado: **pasa**.

**CP-09 · Un pedido ya cerrado no se puede cerrar otra vez**
Se lanza `PedidoNoEditableException` y el estado sigue en `COMPLETADO`.
Resultado: **pasa**.

**CP-10 · Cerrar un pedido que no existe lanza error**
Se lanza `MesaNotFoundException`.
Resultado: **pasa**.

**CP-11 · El pedido pasa a EN_PREPARACION y guarda sus productos**
Se actualiza el estado y la lista de productos con `save` y se comprueba que la
base los conserva. La comparación de `productos` se hace en SQL, porque JSONB
reordena las llaves y cambia los espacios.
Resultado: **pasa** (después de la corrección 1).

### Reglas de la mesa a lo largo del ciclo

**CP-12 · Un pedido CANCELADO no bloquea la mesa**
Con un pedido cancelado en la mesa, `crearPedido` acepta uno nuevo.
Resultado: **pasa**.

**CP-13 · Tras cerrar un pedido la mesa acepta uno nuevo**
Se crea un pedido, se cierra y se crea otro en la misma mesa.
Resultado: **falló en la primera ejecución**, corregido. Ver el defecto 2.

## Defectos encontrados y corregidos

### Defecto 1 · No se podía guardar ningún pedido

`PedidoRepository.save()` enviaba `productos` como texto (`setString`), pero en
la base la columna es `jsonb`. PostgreSQL rechaza la inserción con:

```
ERROR: la columna «productos» es de tipo jsonb pero la expresión es de tipo character varying
```

Impacto: `crearPedido` fallaba siempre contra una base creada con los DDL del
repositorio. En la primera ejecución fallaron 11 de los 13 casos (los únicos que
pasaban eran CP-05 y CP-10, que no guardan nada).

Corrección: se agregó la conversión `?::jsonb` en el `INSERT` y en el `UPDATE`
de `save()`.

### Defecto 2 · Una mesa no aceptaba pedidos nuevos después de cerrar uno

`crearPedido` consultaba si la mesa tenía un pedido con estado distinto de
`CANCELADO`. Un pedido `COMPLETADO` también cumple esa condición, así que
después de cerrar un pedido la mesa quedaba `LIBRE` pero rechazaba cualquier
pedido nuevo con "La mesa N ya tiene una comanda activa".

Pasos para reproducirlo, antes de la corrección:
1. Mesa libre. `crearPedido` → se crea el pedido y la mesa queda OCUPADA.
2. `cerrarPedido` → el pedido queda COMPLETADO y la mesa LIBRE.
3. `crearPedido` en la misma mesa → lanza `MesaOcupadaException`.

Impacto: en el restaurante, cada mesa solo podría atender un pedido.

Corrección: se agregó `PedidoRepository.existeComandaActiva`, que solo
considera activa una comanda que no esté `COMPLETADO`, `ENTREGADO` ni
`CANCELADO`, y `crearPedido` la usa en lugar de la consulta anterior. El método
`existsByMesaIdAndEstadoNot` no se tocó.

## Observaciones para registrar aparte

Estas no se corrigieron porque exceden el alcance de una historia de pruebas.
Conviene abrirlas como issues independientes.

**1. Hay dos enums `EstadoPedido`.** Uno en `entity` (`PENDIENTE`, `ASIGNADO_MESA`,
`EN_PREPARACION`, `COMPLETADO`, `ENTREGADO`, `CANCELADO`) y otro en `enums` con
package `com.restaurante.enums` (`PENDIENTE`, `CONFIRMADO`, `EN_COCINA`,
`CANCELADO`). Los estados de cocina deberían vivir en un solo lugar.

**2. El estado por defecto de la columna `pedidos.estado` es `'ABIERTO'`**, un
valor que ningún enum conoce. `PedidoRepository.findById` fallaría con
`EstadoPedido.valueOf` si algún pedido se insertara sin indicar el estado.

**3. El número de pedido se genera con `System.currentTimeMillis()`.** Dos
pedidos creados en el mismo milisegundo tendrían el mismo número, y la columna
`numero_pedido` es única. No se observó en las pruebas, pero el riesgo existe.

**4. La pantalla "Mesero — Menú y pedidos" aún no registra pedidos.** El botón
"Confirmar pedido" solo valida disponibilidad; el registro está pendiente de
otra historia.

## Resumen de la ejecución

Primera ejecución (antes de las correcciones):

```
Tests run: 13, Failures: 0, Errors: 11, Skipped: 0
```

Segunda ejecución (después de las correcciones):

```
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
```

Con el resto de las pruebas del proyecto: `Tests run: 32, Failures: 0, Errors: 0`,
`BUILD SUCCESS`.

La salida completa está en `docs/pruebas/hu-XXX-ejecucion-antes.txt` y
`docs/pruebas/hu-XXX-ejecucion-despues.txt`.
