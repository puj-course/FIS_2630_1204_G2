# Diagrama de Código C4 – Nivel 4 (Lógica de negocio y Patrones GoF)

## 1. Descripción general
El diagrama de código C4 de GastroFlow correspondiente a la lógica de negocio y patrones GoF muestra las clases de servicio, fachadas, estrategias y patrones de diseño aplicados.

Este nivel detalla cómo se implementan las reglas de negocio y qué patrones de diseño (`Strategy`, `Singleton`, `Observer`, `Factory Method`, `Facade` y `Adapter`) se utilizan.

## 2. Diagrama de código
A continuación, se presenta el diagrama C4 de Nivel 4 – Lógica de negocio y Patrones GoF de GastroFlow.

<img width="3665" height="3050" alt="C4 Nivel 4 - Logica de negocio y Patrones GoF" src="https://github.com/user-attachments/assets/2e06799f-2e81-45ed-a511-64acf7e98729" />

## 3. Organización de las clases
Las clases se organizan en:
- **Servicios de negocio:** `MesaService`, `PedidoService`, `InventarioService`, `ReservaService`, `AutenticacionService`, etc.
- **Patrones GoF:**
  - `Strategy`: Cálculo de descuentos (`DescuentoStrategy` y concreciones).
  - `Singleton`: `SesionUsuario`.
  - `Observer`: Observadores de inventario y alertas.
  - `Factory Method`: Creación de alertas.
  - `Facade`: `PedidoFacade` y servicios relacionados.
  - `Adapter`: Adaptadores de comandas.
- **Clases de apoyo y entidades de dominio.**

## 4. Relaciones entre clases
Los servicios dependen de repositorios y entre sí. Se evidencian claramente las relaciones de los patrones (`Strategy` inyectada en el contexto de descuentos, `Singleton` de sesión, `Observer` notificando cambios de stock, `Factory` creando alertas, etc.).

## 5. Conclusión
El diagrama C4 de Nivel 4 de lógica de negocio permite visualizar tanto las responsabilidades de los servicios como la aplicación concreta de varios patrones GoF, facilitando el entendimiento de la arquitectura de dominio de GastroFlow.
