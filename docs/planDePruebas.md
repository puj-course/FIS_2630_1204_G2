# Plan de pruebas unitarias

Taller de pruebas unitarias con JUnit y métricas con SonarQube
Fundamentos de Ingeniería de Software · GastroFlow

## 1. Análisis del proyecto

### Criterio de selección

GastroFlow tiene 114 clases en `src/main/java`. Para escoger qué probar
aplicamos dos criterios.

El primero es que el componente contenga una decisión de negocio y no solo
paso de datos. Un repositorio que arma un `SELECT` o un controlador que llena
una tabla no deciden nada: trasladan. Los tres componentes que escogimos sí
deciden, y cada uno decide algo que el usuario ve.

El segundo es que la decisión se pueda ejercitar aislada. Revisamos los
`import` de los tres paquetes y ninguno hace referencia a `ConexionBD`, a
`java.sql`, a los repositorios ni a JavaFX; solo dependen de entidades del
dominio y de la librería estándar de Java. Eso es lo que nos permite cumplir
los principios F.I.R.S.T.: las pruebas corren sin PostgreSQL levantado, en
milisegundos, y dan el mismo resultado en cualquier máquina del equipo.

### Los tres componentes

| Componente | Paquete | Clases | Patrón GoF | Lo consume | Qué decide |
|---|---|---|---|---|---|
| Descuentos | `service.descuento` | 4 | Strategy | `CajeroPagoController` | Cuántos pesos se le restan a una cuenta a partir de lo que escribe el cajero |
| Alertas de inventario | `service.alerta` | 8 | Factory Method | `InventarioController` | Si un ingrediente se muestra como NORMAL, STOCK BAJO o AGOTADO |
| Tablero de cocina | `service.cocina` | 2 | Adapter | `CocinaController` | En qué columna aparece una comanda y cuál es su siguiente paso |

Los tres implementan patrones que el equipo ya tenía documentados en `docs/`,
así que probarlos también sirve para verificar que los patrones están bien
aplicados y no solo bien dibujados.

### Métodos seleccionados

**Descuentos.** La unidad mínima es el cálculo de un descuento: entra un
subtotal y un valor, sale un monto en pesos.

| Método | Por qué es unidad mínima |
|---|---|
| `DescuentoPorcentaje.calcular` | Una de las dos fórmulas del sistema; no depende de nada más |
| `DescuentoMontoFijo.calcular` | La otra fórmula; se contrasta con la anterior con la misma entrada |
| `CalculadoraDescuento.calcular` | El contexto del patrón; verifica que delegue y no calcule por su cuenta |
| `CalculadoraDescuento.setEstrategia` | El intercambio en tiempo de ejecución, que es la razón de ser del Strategy |
| `CalculadoraDescuento()` | El constructor fija la estrategia por defecto, que es la que ve el cajero al abrir la pantalla |

Dejamos fuera `getNombre` y `toString` porque devuelven una constante y no hay
decisión que probar.

**Alertas de inventario.** La unidad mínima es la clasificación de un
ingrediente según su stock.

| Método | Por qué es unidad mínima |
|---|---|
| `AlertaStockFactory.para` | Es el que decide; las demás clases solo construyen el objeto que él eligió |
| `AlertaAgotadoFactory.crearAlerta` | Verifica que la fábrica concreta entregue la alerta que corresponde |
| `AlertaStockBajoFactory.crearAlerta` | Lo mismo para el caso intermedio |
| `AlertaNormalFactory.crearAlerta` | Lo mismo para el caso sin alerta |
| `AlertaStock.getEstiloFila` | Cada nivel de alerta pinta la fila del inventario distinto; es la salida que ve el usuario |

Probamos además `Ingrediente.tieneStockBajo`, que no pertenece al paquete de
alertas sino a la entidad, porque es el umbral que `para` consulta para decidir
entre NORMAL y STOCK BAJO. No lo contamos entre los cinco para que los cinco
queden todos dentro del componente, pero los casos CP20 y CP21 lo cubren.

**Tablero de cocina.** La unidad mínima es la traducción de un pedido a lo que
la pantalla necesita mostrar.

| Método | Por qué es unidad mínima |
|---|---|
| `getColumna` | Traduce siete estados posibles a tres columnas |
| `puedeAvanzar` | Decide si la tarjeta lleva botón o no |
| `getTextoAccion` | Decide qué dice ese botón |
| `avanzar` | Es el único que cambia el estado del pedido |
| `getMinutosEspera` | Calcula el tiempo que colorea la tarjeta |

Dejamos fuera `getNumero` y `getMesaId` porque son delegaciones de una línea al
pedido y no transforman nada.

Son 3 componentes y 15 métodos, por encima del mínimo del taller, que es 3 y 5,
cubiertos por 41 casos de prueba.

## 2. Diseño del plan de pruebas

Cada caso se describe con las cinco cosas que pide el taller. Las cuatro
primeras van en la tabla del plan y la quinta, la justificación, va en la
tabla de trazabilidad que sigue a cada una, junto con el método al que
corresponde el caso.

Los tipos que usamos son los del enunciado. *Normal* es el camino esperado,
*borde* es un valor válido justo en el límite, *negativa* es una entrada
inválida y *excepción* es una operación que el estado del objeto no permite.
Un caso puede ser negativo y además terminar en excepción; cuando es así lo
anotamos como "negativa / excepción".

Los resultados reales corresponden a la ejecución sobre la versión corregida
del código. Lo que devolvían antes de corregir está en la sección 4.

### Servicio 1 — Descuentos (`service.descuento`)

| Caso | Descripción | Entrada de datos | Tipo de prueba | Resultados esperados | Resultados reales |
|---|---|---|---|---|---|
| CP01 | Descuento por porcentaje sobre una cuenta corriente | subtotal = 100000, valor = 10 | Normal | 10000.00 | 10000.00 |
| CP02 | Porcentaje en el límite superior permitido | subtotal = 100000, valor = 100 | Borde | 100000.00 | 100000.00 |
| CP03 | Porcentaje por encima del límite | subtotal = 100000, valor = 101 | Negativa / excepción | `IllegalArgumentException` | `IllegalArgumentException` |
| CP04 | Monto fijo igual al subtotal | subtotal = 100000, valor = 100000 | Borde | 100000.00 | 100000.00 |
| CP05 | Monto fijo mayor que la cuenta | subtotal = 20000, valor = 90000 | Negativa / excepción | `IllegalArgumentException` | `IllegalArgumentException` |
| CP06 | Subtotal no informado | subtotal = null, valor = 5000 | Negativa / excepción | `IllegalArgumentException` | `IllegalArgumentException` |
| CP07 | El contexto delega en su estrategia | subtotal = 100000, valor = 15 | Normal | El mismo valor que devuelve la estrategia llamada directamente | Coinciden |
| CP08 | Descuento de cero | subtotal = 100000, valor = 0 | Borde | 0.00 | 0.00 |
| CP09 | Cambio de estrategia en tiempo de ejecución | subtotal = 100000, valor = 10, con las dos estrategias | Negocio | 10.00 y luego 10000.00 | 10.00 y 10000.00 |
| CP10 | Asignar una estrategia nula | estrategia = null | Negativa / excepción | `IllegalArgumentException` | `IllegalArgumentException` |
| CP11 | Estrategia por defecto de la calculadora | sin argumentos | Normal | `DescuentoMontoFijo` | `DescuentoMontoFijo` |
| CP34 | Porcentaje sobre una cuenta sin subtotal | subtotal = null, valor = 10 | Negativa / excepción | `IllegalArgumentException` | `IllegalArgumentException` |
| CP35 | Porcentaje negativo | subtotal = 100000, valor = -5 | Negativa / excepción | `IllegalArgumentException` | `IllegalArgumentException` |
| CP36 | Subtotal negativo con la estrategia de porcentaje | subtotal = -100, valor = 10 | Negativa / excepción | `IllegalArgumentException` | `IllegalArgumentException` |
| CP37 | Monto fijo negativo | subtotal = 100000, valor = -1000 | Negativa / excepción | `IllegalArgumentException` | `IllegalArgumentException` |
| CP38 | Subtotal negativo con la estrategia de monto fijo | subtotal = -100, valor = 10 | Negativa / excepción | `IllegalArgumentException` | `IllegalArgumentException` |
| CP39 | Construir la calculadora con estrategia nula | estrategia = null | Negativa / excepción | `IllegalArgumentException` | `IllegalArgumentException` |

| Caso | Método probado | Justificación del caso |
|---|---|---|
| CP01 | `DescuentoPorcentaje.calcular` | Es el cálculo más frecuente en caja; si falla, toda cuenta con descuento sale mal |
| CP02 | `DescuentoPorcentaje.calcular` | 100% es el máximo válido: hay que confirmar que se acepta y no se rechaza junto con los inválidos |
| CP03 | `DescuentoPorcentaje.calcular` | Un dígito de más al teclear, 101 en vez de 10, no puede producir un descuento mayor que la cuenta |
| CP04 | `DescuentoMontoFijo.calcular` | Cortesía total de la casa: es un caso válido y no debe confundirse con el que lo supera |
| CP05 | `DescuentoMontoFijo.calcular` | Evita un total negativo; la estrategia devolvía los 90000 completos |
| CP06 | `DescuentoMontoFijo.calcular` | Una cuenta sin subtotal cargado debe dar un error con mensaje y no un `NullPointerException` |
| CP07 | `CalculadoraDescuento.calcular` | Verifica la condición del patrón Strategy: el contexto delega y no calcula por su cuenta |
| CP08 | `CalculadoraDescuento.calcular` | Es el estado inicial de la pantalla de caja, donde el campo vacío se lee como cero |
| CP09 | `CalculadoraDescuento.setEstrategia` | El mismo valor debe dar resultados distintos según la estrategia; es la razón de ser del patrón |
| CP10 | `CalculadoraDescuento.setEstrategia` | El error debe salir al asignar y no después dentro de `calcular`, donde el mensaje no diría nada útil |
| CP11 | `CalculadoraDescuento()` | Es la opción que ve el cajero al abrir la pantalla y debe coincidir con la primera del desplegable |
| CP34 | `DescuentoPorcentaje.calcular` | La validación existía en el código pero ninguna prueba la ejercitaba; una cuenta sin subtotal cargado debe dar un error con mensaje |
| CP35 | `DescuentoPorcentaje.calcular` | Un porcentaje negativo aumentaría la cuenta en vez de rebajarla |
| CP36 | `DescuentoPorcentaje.calcular` | Un subtotal negativo solo puede venir de un dato corrupto y el servicio no debe operar con él |
| CP37 | `DescuentoMontoFijo.calcular` | Mismo riesgo que CP35, por la otra estrategia |
| CP38 | `DescuentoMontoFijo.calcular` | Mismo riesgo que CP36, por la otra estrategia |
| CP39 | `CalculadoraDescuento()` | CP10 cubría el cambio de estrategia pero no la construcción; las dos rutas comparten la validación |

Pruebas negativas: CP03, CP05, CP06, CP10 y CP34 a CP39. Pruebas de borde:
CP02, CP04 y CP08. Caso de lógica de negocio: CP09.

### Servicio 2 — Alertas de inventario (`service.alerta`)

| Caso | Descripción | Entrada de datos | Tipo de prueba | Resultados esperados | Resultados reales |
|---|---|---|---|---|---|
| CP12 | Ingrediente con stock holgado | stock = 50, mínimo = 10 | Normal | Alerta NORMAL | NORMAL |
| CP13 | Stock exactamente en cero | stock = 0, mínimo = 10 | Borde | `AlertaAgotadoFactory`, texto AGOTADO | AGOTADO |
| CP14 | Stock exactamente igual al mínimo | stock = 10, mínimo = 10 | Borde | `AlertaStockBajoFactory`, texto STOCK BAJO | STOCK BAJO |
| CP15 | Stock negativo | stock = -5, mínimo = 10 | Negativa | AGOTADO, sin excepción | AGOTADO |
| CP16 | Ingrediente nulo | ingrediente = null | Negativa | `AlertaNormalFactory`, sin excepción | NORMAL |
| CP17 | Ingrediente sin stock registrado | stock = null, mínimo = 10 | Negativa | NORMAL, sin excepción | NORMAL |
| CP18 | Cada fábrica crea su alerta con su estilo | las tres fábricas concretas | Normal | Tipo y estilo propios de cada una | Correcto |
| CP19 | La fábrica entrega instancias nuevas | dos llamadas seguidas | Normal | Dos objetos distintos | Objetos distintos |
| CP20 | Una unidad por encima del mínimo | stock = 11, mínimo = 10 | Borde | No hay alerta | No hay alerta |
| CP21 | Ingrediente sin mínimo configurado | stock = 1, mínimo = null | Negativa | No hay alerta | No hay alerta |
| CP33 | La alerta escala a medida que se consume el ingrediente | stock = 50, 11, 10, 3 y 0, con mínimo = 10 | Negocio | NORMAL, NORMAL, STOCK BAJO, STOCK BAJO y AGOTADO | Correcto |

| Caso | Método probado | Justificación del caso |
|---|---|---|
| CP12 | `AlertaStockFactory.para` | Es el caso mayoritario del inventario; si fallara, la tabla se llenaría de alertas falsas |
| CP13 | `AlertaStockFactory.para` | Cero cumple las dos condiciones a la vez, agotado y bajo mínimo: verifica cuál gana |
| CP14 | `AlertaStockFactory.para` | Define si el umbral es inclusivo, es decir si se alerta al llegar al mínimo o solo al bajar de él |
| CP15 | `AlertaStockFactory.para` | Un descuento de inventario mal registrado puede dejar el stock negativo y la tabla no puede romperse |
| CP16 | `AlertaStockFactory.para` | La tabla recorre filas una por una; una fila sin ingrediente no puede tumbar la pantalla completa |
| CP17 | `AlertaStockFactory.para` | Es el estado de un ingrediente recién creado al que todavía no le cargaron inventario |
| CP18 | Los tres `crearAlerta` | Comprueba el Factory Method: cada subclase entrega su propio producto con su propio estilo |
| CP19 | `AlertaStockBajoFactory.crearAlerta` | Si devolviera siempre la misma instancia, cambiar el estilo de una fila lo cambiaría en todas |
| CP20 | `Ingrediente.tieneStockBajo` | Delimita el umbral por el lado contrario a CP14; entre los dos fijan el punto exacto de corte |
| CP21 | `Ingrediente.tieneStockBajo` | Deja registrado el comportamiento actual: sin mínimo configurado el ingrediente nunca alerta |
| CP33 | `AlertaStockFactory.para` | Sigue a un mismo ingrediente gastándose durante el servicio; ningún otro caso recorre los tres niveles en secuencia, así que un error en el orden de las condiciones pasaría inadvertido |

Pruebas negativas: CP15, CP16, CP17 y CP21. Pruebas de borde: CP13, CP14 y
CP20. Caso de lógica de negocio: CP33.

### Servicio 3 — Tablero de cocina (`service.cocina`)

| Caso | Descripción | Entrada de datos | Tipo de prueba | Resultados esperados | Resultados reales |
|---|---|---|---|---|---|
| CP22 | Cada estado cae en su columna | los cuatro estados que llegan a cocina | Normal | PENDIENTES, PREPARACION o LISTOS según el estado | Correcto |
| CP23 | Pedido sin estado | estado = null | Negativa | Columna PENDIENTES, sin excepción | PENDIENTES |
| CP24 | Estados que tienen siguiente paso | PENDIENTE, ASIGNADO_MESA, EN_PREPARACION | Normal | `true` | `true` |
| CP25 | Pedido ya listo | estado = COMPLETADO | Borde | `false` | `false` |
| CP26 | Pedido fuera del flujo de cocina | PAGADO, ENTREGADO, CANCELADO | Borde | `false` | `false` |
| CP27 | Texto del botón según el estado | PENDIENTE, EN_PREPARACION, COMPLETADO | Normal | "Pasar a preparación", "Marcar como listo", "—" | Correcto |
| CP28 | Recorrido completo del flujo | PENDIENTE y dos avances | Negocio | EN_PREPARACION y luego COMPLETADO | Correcto |
| CP29 | Avanzar un pedido ya entregado | estado = ENTREGADO | Excepción | `PedidoNoEditableException` y el estado no cambia | Correcto |
| CP30 | Comanda construida sin pedido | pedido = null | Negativa / excepción | `IllegalArgumentException` | `IllegalArgumentException` |
| CP31 | Minutos de espera de un pedido reciente | fecha = ahora − 12 minutos | Normal | 12 | 12 |
| CP32 | Pedido sin fecha registrada | fecha = null | Borde | 0, sin excepción | 0 |
| CP40 | Pedido sin estado en el botón de acción | estado = null | Negativa | "—", sin excepción | "—" |
| CP41 | Pedido con fecha futura | fecha = ahora + 30 minutos | Borde | 0, nunca un tiempo negativo | 0 |

| Caso | Método probado | Justificación del caso |
|---|---|---|
| CP22 | `getColumna` | Es el armado del tablero; un error deja comandas en la columna equivocada y la cocina trabaja en desorden |
| CP23 | `getColumna` | El tablero se refresca cada pocos segundos: una fila con el dato incompleto no puede tumbarlo entero |
| CP24 | `puedeAvanzar` | Son los tres estados en que cocina efectivamente trabaja y el botón debe aparecer |
| CP25 | `puedeAvanzar` | Es el límite superior del flujo: ya está listo y no hay paso siguiente |
| CP26 | `puedeAvanzar` | Estados posteriores o cancelados; antes respondían que sí podían avanzar |
| CP27 | `getTextoAccion` | El mesero decide qué hacer leyendo el botón, así que el texto es parte de la lógica y no decoración |
| CP28 | `avanzar` | Recorre el ciclo de vida completo de una comanda en una sola prueba |
| CP29 | `avanzar` | Comprueba que el servicio se proteja solo, sin depender de que la consulta no traiga esos pedidos |
| CP30 | Constructor de `PedidoComandaAdapter` | El error debe salir al construir y no más tarde, en cualquiera de los métodos |
| CP31 | `getMinutosEspera` | El tiempo de espera colorea la tarjeta y es la señal de alerta que mira la cocina |
| CP32 | `getMinutosEspera` | Antes lanzaba `NullPointerException` y tumbaba el refresco completo del tablero |
| CP40 | `getTextoAccion` | CP23 cubría el estado nulo en `getColumna` pero no aquí, que tiene su propia guarda |
| CP41 | `getMinutosEspera` | Con el reloj de una caja adelantado el tablero mostraba un tiempo de espera negativo |

Pruebas negativas o de error: CP23, CP29, CP30 y CP40. Pruebas de borde: CP25,
CP26, CP32 y CP41. Caso de lógica de negocio: CP28.

### Resumen de cobertura de casos

Cada caso se cuenta una sola vez, por su tipo principal.

| Servicio | Casos | Normal | Borde | Negativa | Excepción | Negocio |
|---|---|---|---|---|---|---|
| Descuentos | 17 | 3 | 3 | 10 | 0 | 1 |
| Alertas de inventario | 11 | 3 | 3 | 4 | 0 | 1 |
| Cocina | 13 | 4 | 4 | 3 | 1 | 1 |
| **Total** | **41** | **10** | **10** | **17** | **1** | **3** |

Catorce de esos casos terminan con el método lanzando una excepción: CP03,
CP05, CP06, CP10, CP29, CP30 y CP34 a CP39. Trece son entradas inválidas y por
eso quedan clasificados como negativos; CP29 es el único donde la entrada es
válida y lo que impide la operación es el estado del objeto, así que lo
clasificamos como excepción.

El reparto no es parejo entre servicios y tiene una razón. Descuentos acumula
diez casos negativos porque sus dos estrategias validan cuatro condiciones cada
una, y todas tienen que quedar cubiertas. Cocina reparte entre borde y
negativa porque buena parte de su lógica son estados de un enumerado, donde el
límite del flujo importa más que la entrada inválida.

### Cobertura de casos especiales

El taller pide, por cada servicio, dos pruebas negativas entendidas como caso
de error o excepción, dos con datos límite o borde, y una que siga la lógica
de negocio propia de la aplicación. Los tres servicios cumplen.

| Servicio | Dos negativas o de excepción | Dos de borde | Una de lógica de negocio |
|---|---|---|---|
| Descuentos | CP03, CP05, CP06, CP10, CP34 a CP39 | CP02, CP04, CP08 | CP09 |
| Alertas de inventario | CP15, CP16, CP17, CP21 | CP13, CP14, CP20 | CP33 |
| Tablero de cocina | CP23, CP29, CP30, CP40 | CP25, CP26, CP32, CP41 | CP28 |

Sobre los casos de lógica de negocio conviene explicar por qué son esos. En
descuentos es CP09, el cajero que cambia de porcentaje a monto fijo en plena
venta, que es la situación concreta para la que el equipo introdujo el patrón
Strategy. En cocina es CP28, el recorrido completo de una comanda desde que el
mesero la envía hasta que la cocina la marca lista. En alertas es CP33, donde
seguimos a un mismo ingrediente gastándose a lo largo del servicio: los demás
casos miran un nivel por separado y entran cada uno por una rama distinta del
método, de modo que un error en el orden de las condiciones no lo detectaría
ninguno.

## 3. Implementación

Las pruebas están en `src/test/java/service/`, una clase por servicio:

- `service/descuento/CalculadoraDescuentoTest.java`
- `service/alerta/AlertaStockFactoryTest.java`
- `service/cocina/PedidoComandaAdapterTest.java`

Cada prueba sigue el patrón Arrange - Act - Assert, con las tres secciones
marcadas con comentarios. Sobre los principios F.I.R.S.T.:

- **Fast**: ninguna prueba abre conexión a la base de datos.
- **Independent**: cada prueba construye sus propios objetos; no hay estado
  compartido ni orden obligatorio.
- **Repeatable**: no dependen del entorno. La única prueba que usa la hora del
  sistema, CP31, mide una diferencia y no un instante.
- **Self-validating**: cada una termina en una aserción que pasa o falla sola.
- **Timely**: las escribimos junto con la corrección del defecto, no después.

### Cobertura de las validaciones

Al revisar el código contamos quince validaciones defensivas en los tres
servicios: comprobaciones de argumento nulo, de valor negativo, de límite
superado y de estado que no admite la operación. Siete de ellas no tenían
ninguna prueba que las ejercitara, es decir, eran ramas escritas y nunca
recorridas. Los casos CP34 a CP41 las cubren, y con eso las quince quedan
con al menos una prueba detrás.

Lo anotamos porque es una fuente de cobertura perdida que no se ve en el
conteo de pruebas: el servicio parecía probado porque tenía once casos, pero
casi la mitad de sus validaciones nunca se ejecutaba.

## 4. Defectos encontrados y corregidos

| # | Dónde | Qué pasaba | Caso que lo detecta | Corrección |
|---|---|---|---|---|
| 1 | `PedidoComandaAdapter.puedeAvanzar` | La regla estaba escrita al revés: "cualquier estado menos COMPLETADO". Un pedido ENTREGADO, PAGADO o CANCELADO respondía que sí podía avanzar, y `avanzar()` lo devolvía a EN_PREPARACION | CP26, CP29 | Se enumeran los tres estados que sí tienen paso siguiente, y `avanzar()` lanza `PedidoNoEditableException` en los demás |
| 2 | `DescuentoMontoFijo.calcular` y `DescuentoPorcentaje.calcular` | Ninguna de las dos estrategias validaba nada. Un monto fijo mayor que la cuenta se devolvía completo y un porcentaje mayor a 100 pasaba sin objeción | CP03, CP05, CP06 | La validación quedó dentro de cada estrategia, que es donde se puede probar y donde protege a cualquier llamador |
| 3 | `CajeroPagoController.recalcular` | El contexto del patrón Strategy estaba fuera del camino de ejecución: `calculadoraDescuento` se construía, recibía la estrategia y nunca se usaba, porque el cálculo se le pedía directamente a la estrategia del desplegable | CP07, CP09 | El controlador ahora calcula a través del contexto |
| 4 | `PedidoComandaAdapter.getMinutosEspera` | Un pedido sin fecha lanzaba `NullPointerException` y tumbaba el refresco completo del tablero, que corre cada pocos segundos | CP32 | Devuelve 0 cuando no hay fecha |
| 5 | `CalculadoraDescuento.setEstrategia` | Aceptaba null y el fallo aparecía después, dentro de `calcular`, como un NPE sin contexto | CP10 | Se rechaza en el momento en que se comete el error |

Sobre el defecto 1 y el 2 hay un matiz que vale la pena anotar: en la
aplicación no eran visibles, porque la consulta `findPedidosParaCocina` nunca
trae pedidos entregados y porque `CajeroPagoController` tenía sus propios `if`
para frenar un descuento demasiado alto. El problema era que esas reglas
vivían fuera del servicio: en una consulta SQL y en un controlador de JavaFX,
los dos sitios donde una prueba unitaria no llega. Cualquier otro llamador de
estos servicios no habría tenido ninguna protección.

## 5. Hallazgo abierto

`Ingrediente.tieneStockBajo` devuelve `false` cuando el ingrediente no tiene
stock mínimo configurado (CP21). Un ingrediente con una sola unidad en bodega
no genera ninguna alerta si nadie le definió el mínimo. Lo dejamos documentado
en lugar de corregirlo porque la decisión es de negocio: hay que definir si el
mínimo es obligatorio al crear el ingrediente o si existe un valor por defecto.
