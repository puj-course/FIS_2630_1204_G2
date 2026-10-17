# Cuatro hallazgos del código, listos para abrir como issues

Salieron de revisar el código para construir los diagramas de estados de la
HU-129. Ninguno es un problema de los diagramas: son del código, y cada uno
está comprobado contra la rama `develop` en el commit `084c099`
(2026-10-15).

Van ordenados por urgencia: el primero tiene la suite en rojo ahora mismo,
el segundo corrompe la base de desarrollo, el tercero está latente y el
cuarto es alcanzable desde la interfaz.

---

## Issue 1 — CP-07 y CP-09 esperan COMPLETADO donde el servicio escribe ENTREGADO

**Tipo:** bug · pruebas
**Prioridad:** alta — la suite está fallando

### Qué pasa

`PedidoService.cerrarPedido` deja el pedido en `ENTREGADO`
(`src/main/java/service/PedidoService.java:59`), pero dos casos de prueba
esperan `COMPLETADO`:

- `CP-07 · Al cerrar el pedido queda COMPLETADO`
  (`src/test/java/repository/PedidoModuloFuncionalTest.java:213`), con dos
  aserciones en las líneas 219 y 220.
- `CP-09 · Un pedido ya cerrado no se puede cerrar otra vez` (`:239`), en su
  última aserción (`:248`).

### Por qué importa

`mvn test` falla, así que cualquier integración continua que ejecute las
pruebas queda en rojo y deja de servir como red de seguridad para el resto
del equipo.

### Qué habría que decidir

No está claro cuál de los dos lados es el correcto, y conviene decidirlo
antes de tocar nada:

- Si el estado final al cerrar debe ser `ENTREGADO`, hay que actualizar las
  dos pruebas. También hay que corregir el mensaje de la interfaz, que dice
  «El pedido se marcará como completado»
  (`src/main/java/controller/MenuMeseroController.java:623`).
- Si debe ser `COMPLETADO`, hay que cambiar `PedidoService:59`, y entonces
  `COMPLETADO` pasaría a ser un estado final, lo que obliga a revisar
  `ESTADOS_YA_CERRADOS` (`PedidoService.java:16-19`) y la consulta
  `findPedidosParaCocina` (`PedidoRepository.java:224-232`), que hoy carga
  `COMPLETADO` como pedido activo.

Hay una incoherencia de fondo que conviene resolver de paso: el código trata
`COMPLETADO` de dos formas distintas. `existsByMesaIdAndEstadoNot` lo
considera no activo (`PedidoRepository.java:44`) y `findActivoByMesaId` lo
considera activo (`:191`).

### Criterio de aceptación

`mvn test` pasa, y el estado que escribe el servicio, el que esperan las
pruebas y el que anuncia la interfaz son el mismo.

---

## Issue 2 — Las pruebas dejan `LIBRE` en el catálogo `estados_mesa` y no lo limpian

**Tipo:** bug · pruebas · datos
**Prioridad:** alta — cambia el comportamiento de la aplicación

### Qué pasa

Las dos pruebas funcionales siembran el catálogo de estados en su
`@BeforeAll`:

- `MesaModuloFuncionalTest.java:64` llama a `asegurarEstados` (`:347`), que
  inserta `LIBRE`, `OCUPADA` y `RESERVADA`.
- `PedidoModuloFuncionalTest.java:69` hace lo mismo (`:379`).

Sus `@AfterAll` borran mesas, zonas, pedidos e historial
(`MesaModuloFuncionalTest.java:78-85`,
`PedidoModuloFuncionalTest.java:83-93`), pero **no borran nada de
`estados_mesa`**. Basta correr `mvn test` una vez para que `LIBRE` quede como
fila permanente del catálogo.

El catálogo que instala el proyecto tiene otros cuatro códigos y no incluye
`LIBRE` (`db/functions/generar_estado_mesa_prueba.ddl:7-10`): `DISPONIBLE`,
`OCUPADA`, `RESERVADA` y `MANTENIMIENTO`.

### Por qué importa

La aplicación se comporta distinto según si alguien ha corrido las pruebas
contra esa base:

- `MesaRepository.liberarMesa` resuelve el estado con
  `obtenerIdEstado(conn, "LIBRE", "DISPONIBLE")` (`:453`), y ese método
  devuelve **el primero que exista** (`:428-441`). Sin `LIBRE` la mesa queda
  en `DISPONIBLE`; con `LIBRE` queda en `LIBRE`.
- El trigger `trg_limpiar_nota_al_liberar_mesa` solo reacciona al código
  `'DISPONIBLE'` (`db/ddl/nota_mesa.ddl:33`), así que una mesa liberada a
  `LIBRE` se queda con su nota pegada.
- `ReservaService.cancelarReserva` deja de lanzar excepción (ver Issue 3).

Es decir: dos entornos con el mismo código se comportan distinto según su
historial de ejecución de pruebas, que es muy difícil de diagnosticar.

### Propuesta

Dos cosas, independientes entre sí:

1. Que el `@AfterAll` de las dos pruebas deje el catálogo como lo encontró, o
   que las pruebas usen una base desechable en vez de la de desarrollo.
2. Decidir si `LIBRE` debe existir o no. Hoy el código lo pide en tres sitios
   y el catálogo no lo tiene: o se añade a `generar_estado_mesa_prueba.ddl` o
   se elimina del código (ver Issue 3).

### Criterio de aceptación

Después de `mvn test`, `SELECT codigo_estado FROM estados_mesa` devuelve
exactamente los códigos que siembra el instalador.

---

## Issue 3 — `cancelarReserva` pide un estado que no existe y deja la reserva a medias

**Tipo:** bug
**Prioridad:** media — el método no tiene llamadores todavía

### Qué pasa

`ReservaService.cancelarReserva` libera la mesa con el literal `"LIBRE"`:

```java
// src/main/java/service/ReservaService.java:53
mesaService.cambiarEstado(reserva.getIdMesa(), "LIBRE", "CANCELAR_RESERVA");
```

`MesaService.cambiarEstado` busca el código antes de tocar la base y lanza si
no lo encuentra (`src/main/java/service/MesaService.java:45-47`):

```java
EstadoMesa nuevo = estadoMesaRepository.findByCodigoEstado(codigoEstadoNuevo)
        .orElseThrow(() -> new MesaNotFoundException(
                "Estado no encontrado: " + codigoEstadoNuevo));
```

Como `LIBRE` no está en el catálogo del instalador, la llamada lanza
`MesaNotFoundException`.

### Por qué importa

El problema no es solo que falle, es **dónde** falla. En la línea 50 ya se
ejecutó y confirmó `reservaRepository.cancelar(idReserva, ahora)`, en su
propia conexión. Cuando la línea 53 lanza, la reserva ya está cancelada y la
mesa se queda en `RESERVADA` para siempre. No hay transacción que abarque las
dos operaciones.

Hoy el daño es teórico porque **`cancelarReserva` no tiene ningún llamador**:
ni controlador, ni `.fxml`, ni prueba. Conviene arreglarlo antes de que
alguien conecte el método a una pantalla.

Vale la pena comprobar de paso si la funcionalidad de reservas está completa:
no hay ningún `INSERT` sobre la tabla `reservas` en todo el código Java, así
que no existe el camino para crear una reserva.

### Propuesta

- Usar un código que exista en el catálogo, o añadir `LIBRE` a la semilla y
  al trigger de `nota_mesa`. Hay que elegir una de las dos: hoy conviven
  `LIBRE` y `DISPONIBLE` como si fueran el mismo estado, y la interfaz los
  trata como equivalentes (`MapaSalonController.java:799`).
- Que las dos escrituras ocurran en la misma transacción, para que una
  reserva no pueda quedar cancelada con la mesa sin liberar.

### Criterio de aceptación

Cancelar una reserva deja la reserva inactiva y la mesa disponible, o no
cambia ninguna de las dos cosas.

---

## Issue 4 — El tablero de cocina escribe el estado saltándose `PedidoService`

**Tipo:** bug · concurrencia
**Prioridad:** media — alcanzable desde la interfaz

### Qué pasa

`CocinaController.avanzarEstado` cambia el estado y guarda directamente por
el repositorio (`src/main/java/controller/CocinaController.java:139-146`):

```java
if (pedido.getEstado() == EstadoPedido.EN_PREPARACION) {
    pedido.setEstado(EstadoPedido.COMPLETADO);
} else {
    pedido.setEstado(EstadoPedido.EN_PREPARACION);
}
pedidoRepository.save(pedido);
```

No pasa por `PedidoService`, así que **no se aplica la guarda
`ESTADOS_YA_CERRADOS`** que protege `cerrarPedido` y `cancelarPedido`
(`PedidoService.java:16-19`, `:53`, `:79`).

### Por qué importa

El tablero se refresca cada 30 segundos (`CocinaController.java:46`). Si un
pedido se cierra o se cancela desde la pantalla del mesero durante ese
intervalo, la tarjeta sigue en la pantalla de cocina con el estado antiguo en
memoria. Al pulsarla, el `else` escribe `EN_PREPARACION` encima y **resucita
un pedido ya entregado o cancelado**.

El efecto es que `ENTREGADO` y `CANCELADO` no son estados finales de verdad:
lo son por intención del servicio, no por garantía del repositorio.

### Propuesta

Tres opciones, de menos a más trabajo:

1. Que `avanzarEstado` llame a un método de `PedidoService` que aplique la
   misma guarda que los otros dos.
2. Releer el pedido por id antes de escribir, y descartar la acción si el
   estado ya cambió.
3. Mover la guarda al repositorio, para que ninguna ruta pueda saltársela.

La primera es la coherente con el resto del diseño: hoy `PedidoService` es el
único sitio que valida transiciones, y cocina es la única excepción.

### Criterio de aceptación

Desde `ENTREGADO` o `CANCELADO` no se puede volver a `EN_PREPARACION` por
ninguna ruta, incluida la pantalla de cocina con datos desactualizados.

---

## De dónde sale esto

Los cuatro se detectaron construyendo los diagramas de estados del pedido y
de la mesa (`docs/diagramaEstados.md`), que documentan tres de ellos en el
propio dibujo: las flechas azules son justamente las escrituras que se saltan
la capa de servicio, y el rojo marca el código `LIBRE` que el catálogo no
tiene.

| Dato | Valor |
|---|---|
| Rama | `develop` |
| Commit revisado | `084c099` (084c099423b4bdd827894e4907aca8b943b1a963) |
| Fecha del commit | 2026-10-15 |
