# Diagrama de Código C4 – Nivel 4 (Acceso a datos)

## 1. Descripción general
El diagrama de código C4 de GastroFlow correspondiente al acceso a datos muestra los repositorios y DAOs responsables de la persistencia.

Este nivel detalla cómo se obtienen las conexiones a la base de datos y cómo se mapean los resultados a las clases del paquete `entity`.

## 2. Diagrama de código
A continuación, se presenta el diagrama C4 de Nivel 4 – Acceso a datos de GastroFlow.

<img width="3197" height="2089" alt="C4 Nivel 4 - Acceso a datos" src="https://github.com/user-attachments/assets/e828acde-52a5-442a-b11f-e7f002bfdaf5" />

## 3. Organización de las clases
Se identifican los siguientes repositorios/DAOs principales:
- `MesaRepository`, `ReservaRepository`, `PedidoRepository`
- `IngredienteRepository`, `InventarioRepository`, `MovimientoInventarioRepository`
- `UsuarioRepository`, `ClienteRepository`, `DetallePedidoRepository`
- `ListaEsperaRepository`, `NotaMesaRepository`, `PedidoHistorialRepository`
- `AsignacionMesaRepository`, `HistorialEstadoMesaRepository`
- `ReglaDescuentoRepository`, `ZonaRepository`, `EstadoMesaRepository`
- `EntradaInventarioRepository`, `AlertaInventarioRepository`
- `PlatoRepository`, `AdicionalRepository`, `ProductoDAO`, `DisponibilidadDAO`, `PagoDAO`, etc.

Todos los repositorios obtienen la conexión desde `ConexionBD` y devuelven clases del paquete `entity`.

## 4. Relaciones entre clases
Los repositorios dependen de `ConexionBD` y de las entidades. Algunos repositorios mantienen relaciones entre sí o utilizan consultas SQL personalizadas (`SELECT_BASE`, `JOINs`, etc.).

## 5. Conclusión
El diagrama C4 de Nivel 4 de acceso a datos permite visualizar la capa de persistencia de GastroFlow, los repositorios existentes y el patrón de obtención de conexiones y mapeo a entidades.
