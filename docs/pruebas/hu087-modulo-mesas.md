# HU-087 · Pruebas funcionales del módulo de mesas

Documento de casos de prueba y resultados.

| | |
|---|---|
| Módulo | Mesas (`repository.MesaRepository`, tabla `mesas`) |
| Pruebas | `src/test/java/repository/MesaModuloFuncionalTest.java` |
| Base usada | PostgreSQL 16, base creada desde cero con los DDL y migraciones del repositorio |
| Resultado | 14 casos ejecutados, 14 pasan. Se encontró y corrigió 1 defecto |

## Cómo ejecutarlas

```bash
mvn test -Dtest=MesaModuloFuncionalTest
```

Son pruebas de integración: necesitan una base PostgreSQL accesible con las
variables de conexión configuradas. Buena parte de las reglas del módulo
—capacidad frente a comensales, catálogo de estados— vive en el SQL y no en
código Java, así que probarlas con objetos simulados no demostraría nada.

Si no hay base disponible, la clase entera se omite en lugar de fallar, para que
quien compile sin base no vea el build en rojo.

Las pruebas crean sus propios datos: una zona `ZONA PRUEBAS HU-087` y dos mesas
con números 9001 y 9002, y los borran al terminar. No tocan las mesas reales.

## Datos de prueba

| Mesa | Número | Capacidad | Para qué sirve |
|---|---|---|---|
| A | 9001 | 4 | Caso general y ciclos de atención |
| B | 9002 | 2 | Valores límite de capacidad |

Antes de cada caso las dos mesas se dejan en estado `LIBRE`, sin comensales y
con su capacidad original.

## Casos de prueba

### Visualización

**CP-01 · Las mesas activas se cargan y se listan**
Se llama `obtenerTodas()` y se comprueba que devuelve mesas, que la mesa de
prueba está entre ellas y que ninguna aparece repetida.
Resultado: **pasa**.

**CP-02 · Cada mesa trae número, capacidad y estado**
Se lee la mesa A y se verifican número, capacidad, código de estado y nombre de
zona.
Resultado: **pasa**. Devuelve número 9001, capacidad 4, estado LIBRE, zona
ZONA PRUEBAS HU-087.

### Asignación y estados

**CP-03 · Se asigna una mesa disponible**
Mesa A libre, se asignan 3 comensales.
Resultado: **pasa**. Devuelve `true` y la base queda con 3.

**CP-04 · Una mesa ocupada no se puede volver a asignar**
Mesa A con 2 comensales y estado OCUPADA. Se intenta una segunda asignación de
3 comensales.
Resultado: **falló en la primera ejecución**, corregido. Ver el defecto abajo.

**CP-05 · Al liberar, la mesa vuelve a LIBRE**
Mesa A ocupada con 4 comensales, se llama `liberarMesa`.
Resultado: **pasa**.

**CP-06 · El estado pasa de LIBRE a OCUPADA al asignar**
Se comprueba el estado antes y después de asignar.
Resultado: **pasa**.

**CP-07 · Al liberar, la cantidad de comensales queda vacía**
Resultado: **pasa**. El campo queda en NULL, no en cero.

### Capacidad y valores límite

**CP-09 · Comensales por encima de la capacidad**
Mesa B de capacidad 2, se intentan 3 comensales.
Resultado: **pasa**. Devuelve `false`, no guarda cantidad y la mesa sigue LIBRE.

**CP-10 · Comensales iguales a la capacidad**
Mesa B de capacidad 2, se asignan 2.
Resultado: **pasa**. El límite es inclusivo, como debe ser.

**CP-11a · Cero comensales**
Resultado: **pasa**. Lanza `IllegalArgumentException` con el mensaje
"La cantidad de comensales debe ser mayor a cero."

**CP-11b · Cantidad negativa (-1)**
Resultado: **pasa**. Lanza la misma excepción.

**CP-11c · Capacidad más uno (5 en mesa de 4)**
Resultado: **pasa**. Devuelve `false` y no guarda nada.

**CP-14 · Bajar la capacidad por debajo de los comensales**
Mesa A con 4 comensales, se intenta dejar la capacidad en 2.
Resultado: **pasa**, pero conviene leer la observación de abajo. La base lo
impide con la restricción `ck_mesas_comensales_capacidad`.

### Consistencia

**CP-12 · Lo que muestra el repositorio coincide con la base**
Se compara lo que devuelve `findById` con una consulta SQL directa sobre
`mesas` y `estados_mesa`: número, capacidad, comensales y estado.
Resultado: **pasa**, los cuatro campos coinciden.

**CP-13 · Cinco ciclos seguidos de asignación y liberación**
Se repite asignar y liberar cinco veces con cantidades distintas, comprobando
estado y comensales en cada paso.
Resultado: **pasa**. No hay arrastre de estado entre ciclos.

## Defecto encontrado y corregido

**Una mesa ocupada aceptaba una segunda asignación**

`asignarComensales` comprobaba la capacidad pero no el estado de la mesa. El
`UPDATE` solo tenía `WHERE id_mesa = ? AND ? <= capacidad`.

Pasos para reproducirlo, antes de la corrección:

1. Mesa 9001, capacidad 4, estado LIBRE.
2. `asignarComensales(9001, 2)` → devuelve `true`, la mesa queda OCUPADA con 2.
3. `asignarComensales(9001, 3)` → **devuelve `true`** y la mesa pasa a 3.

Impacto: en el restaurante, sentar una segunda vez una mesa ya ocupada
sobrescribía en silencio los comensales de la primera, sin ningún aviso. La
cuenta y el aforo quedaban mal.

Corrección: se añadió al `UPDATE` la condición de que la mesa esté libre, con
la misma tolerancia de nombres del catálogo que ya usaba el resto de la clase
(`LIBRE` o `DISPONIBLE`). Ahora el método devuelve `false` y quien llama puede
avisar al usuario.

Después de la corrección, el CP-04 pasa: la segunda asignación se rechaza y los
comensales originales se conservan.

## Observaciones para registrar aparte

Estas dos no se corrigieron en esta historia, porque exceden el alcance de una
historia de pruebas. Conviene abrirlas como issues independientes.

**1. Corregir el número de comensales obliga a liberar la mesa.**
Con la corrección del CP-04, la única forma de cambiar los comensales de una
mesa ocupada es liberarla y volver a asignarla, lo que de paso borra su estado.
Lo razonable sería separar dos operaciones: asignar, solo sobre mesa libre, y
actualizar la cantidad, permitida sobre mesa ocupada. Toca el repositorio, el
controlador y la interfaz.

**2. `cambiarCapacidadMesa` falla con una excepción de base de datos.**
El método no valida nada en Java: cuando la capacidad nueva queda por debajo de
los comensales, el error llega como `SQLException` con el texto de la
restricción de PostgreSQL. Funciona, pero el usuario ve un mensaje técnico. La
validación debería estar antes, en Java, con un mensaje claro.

## Resumen de la ejecución

```
TOTAL: 31 comprobaciones, 31 pasan, 0 fallan
```

La salida completa está en `docs/pruebas/hu087-ejecucion.txt`.
