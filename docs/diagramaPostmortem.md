# Postmortem · Milestone Definición arquitectónica y gestión funcional

![Retrospectiva Starfish del milestone Definición arquitectónica y gestión funcional](Imagenes/DiagramaPostmortem_Starfish.png)

**Por qué está.** Cierra el seguimiento postmortem de la milestone (HU-169). La retrospectiva Starfish, de Patrick Kua, usa cinco categorías en lugar de las tres habituales porque separa lo binario de lo gradual: una práctica que funciona a medias no tiene que eliminarse entera ni callarse, tiene dónde caer. Las dos categorías del medio, hacer más y hacer menos, son las que justifican el formato.

**Qué muestra.** Las cinco categorías con lo que el equipo acordó en la sesión, sobre lo ocurrido en esta milestone: la documentación del diseño, la optimización del modelo de datos, la aplicación de los seis patrones GoF y la integración entre los módulos de mesas, pedidos, inventario, cocina y caja.

## Continuar

- Revisar el código entre compañeros antes de integrar.
- Mantener la comunicación del grupo en las dailys, plannings, reviews y retrospectivas.

Revisar entre compañeros es lo que más rindió. Gracias a eso se detectó que uno de los patrones no correspondía al catálogo GoF y se rehizo antes de la entrega.

## Hacer más

- Probar contra la base antes de dar una historia por terminada.
- Revisar la coherencia entre los diagramas, el modelo de datos y la implementación.

Los fallos más visibles de la milestone aparecieron al cruzar módulos, no dentro de una pantalla: cerrar un pedido dejaba la mesa ocupada, la cantidad de comensales no se guardaba y la caja mostraba las cuentas en cero. Ninguno se habría detectado sin ejecutar el flujo completo contra la base.

## Empezar a hacer

- Crear los diagramas aplicando la notación estándar UML.
- Actualizar la documentación junto con el código.
- Hacer menos uso de la inteligencia artificial.

La documentación se mantuvo al día mientras se generó desde el código y se verificó contra él; se desactualizó en cuanto se editó por fuera de ese camino.

## Hacer menos

- Integrar cuando el proyecto no compila.
- Dar por terminada una historia sin haberla ejecutado.

El proyecto estuvo tres días sin compilar y se siguió integrando encima. La revisión automática que corre en cada cambio ya existía: el problema no fue que faltara, sino que no se miró antes de mezclar.

## Dejar de hacer

- Rehacer trabajo que otro compañero ya hizo.
- Dar por cerrada la documentación antes de que el código deje de moverse.
- Subestimar tareas con alta complejidad técnica.

Dos integrantes corrigieron lo mismo con días de diferencia, y los diagramas hubo que rehacerlos varias veces porque seguían llegando cambios al código.

## Aciertos, dificultades y aprendizajes

Lo que salió bien fue la base de datos y el reparto del trabajo. El esquema concentra las reglas en restricciones propias, las operaciones que tocan varias tablas van en transacción con bloqueo de fila, los cambios de esquema quedaron como migraciones numeradas ligadas a su historia, y los seis patrones se repartieron entre varias personas en lugar de recaer en una.

Lo que costó fue la integración. Los tres defectos más graves estuvieron en los límites entre módulos, y dos de los seis patrones quedaron incompletos al entregarlos: uno no llegó a ejecutarse nunca y otro no seguía el camino que él mismo definía.

El aprendizaje es que en esta milestone se dio por terminado lo que no se había ejecutado. Casi todo lo anterior sale de ahí.

## Acciones para el siguiente ciclo

1. No integrar a `develop` con la revisión automática en rojo.
2. Ninguna historia se marca como terminada sin haberla ejecutado contra la base.
3. Toda historia que añada lógica entrega al menos una prueba automatizada.
4. Un patrón no cuenta como implementado hasta que exista código que lo invoque.
5. Avisar en qué archivo va a trabajar cada uno antes de empezar.
