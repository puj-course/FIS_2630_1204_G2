 Postmortem Milestone: Control del estado y asignación de mesas

## Diagrama de estrella de mar

![Diagrama de estrella de mar del milestone Control del estado y asignación de mesas](Imagenes/DiagramaPostmortem_Starfish_4.png)

## Contexto

El milestone agrupó 16 historias de usuario (HU-041 a HU-059) sobre el mapa interactivo del salón, el estado de las mesas, las reservas, la lista de espera y la asignación de mesas a meseros.

## Seguir haciendo

- Dividir el milestone en historias pequeñas con un solo objetivo cada una, lo que permitió repartir el trabajo entre los cuatro integrantes.
- Priorizar con etiquetas (Medium/High), dejando con prioridad alta las historias más críticas: lista de espera y reservas.
- Cubrir la lógica de mesas y reservas con pruebas y excepciones propias (`MesaOcupadaException`, `MesaConPedidoActivoException`, `ReservaNoCancelableException`).

## Empezar a hacer

- Usar un formato único para los títulos de las historias.
- Agregar etiquetas de tipo (`feature`, `bug`, `mejora`) además de "Diseño".
- Escribir criterios de aceptación en cada issue y revisarlos antes de cerrarla.
- Probar el flujo completo antes de cerrar el milestone.
- Mostrar mensajes de error claros al usuario cuando una operación falla.

## Parar de hacer

- Dejar el trabajo para las últimas fechas: varias issues se cerraron el mismo día, lo que sugiere integración en tandas.
- Duplicar etiquetas o dejarlas sin revisar.
- Concentrar demasiada lógica en una sola clase (`MapaSalonController` supera las 900 líneas).

## Hacer más

- Revisiones cruzadas de código entre integrantes usando la plantilla de PR del repositorio.
- Pruebas automáticas para casos de borde: mesa ocupada, mesa reservada, cancelación y cambio automático de estado.
- Registro del avance en las issues, indicando quién la tomó y en qué punto va.
- Commits pequeños y frecuentes con Conventional Commits y número de HU.

## Hacer menos

- Cambios grandes en un solo commit o PR.
- Conflictos de merge por editar los mismos archivos entre varias personas.
- Decisiones de diseño tomadas a última hora, sin acordarlas en el equipo.
- Revisiones manuales repetitivas que el CI puede automatizar.
