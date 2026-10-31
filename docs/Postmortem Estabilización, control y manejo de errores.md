# Postmortem · Milestone Estabilización, control y manejo de errores

<img width="1188" height="841" alt="Diagrama Postmortem Estabilización, control y manejo de errores" src="https://github.com/user-attachments/assets/72bf44dd-864f-41f0-b928-d4dd20ebee3f" />

## Continuar

- Priorizar oportunamente los errores bloqueantes dentro del backlog sprint tras sprint.
- Mantener la comunicación fluida y rápida en el equipo.

## Hacer más

- Realizar code reviews enfocados explícitamente en el manejo de excepciones y bloques try/catch.
- Reforzar las pruebas de estrés y datos inválidos.

## Empezar a hacer

- Implementar pruebas de integración automatizadas para validar escenarios de fallo y bordes en GastroFlow.
- Adoptar herramientas de monitoreo de errores en tiempo real para detectar excepciones.

## Hacer menos

- Parches improvisados sin análisis profundo.
- Dejar el manejo de excepciones para la fase final del desarrollo.
- Subestimar la complejidad de tareas de refactorización.

## Dejar de hacer

- Ignorar o capturar excepciones en silencio
- Exponer fallos técnicos.
- Validar las historias de usuario omitiendo datos inválidos.

