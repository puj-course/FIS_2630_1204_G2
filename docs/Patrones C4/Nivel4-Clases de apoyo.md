# Diagrama de Código C4 – Nivel 4 (Clases de apoyo)

## 1. Descripción general
El diagrama de código C4 de GastroFlow correspondiente a las clases de apoyo muestra DTOs, utilidades, la clase principal y componentes transversales.

Este nivel detalla las clases de soporte que facilitan la comunicación entre capas, el manejo de sesión y utilidades generales.

## 2. Diagrama de código
A continuación, se presenta el diagrama C4 de Nivel 4 – Clases de apoyo de GastroFlow.

<img width="3092" height="1633" alt="C4 Nivel 4 - Clases de apoyo" src="https://github.com/user-attachments/assets/c9e59d58-eb61-4def-ae8a-b28e799bdb50" />


## 3. Organización de las clases
Las clases se agrupan en:
- **DTOs:** `CuentaPago`, `ItemPedido`, `ProductoDisponibilidad`, `IngredienteStockDTO`
- **Clase principal:** `Main`
- **Sesión:** `SesionUsuario` (Singleton)
- **Utilidades:** `EstadoFormatterUtil`, `TextoBusqueda`
- **Conexión:** `ConexionBD`
- **Interfaces:** `Observador` (para el patrón Observer)

## 4. Relaciones entre clases
`Main` utiliza `SesionUsuario`. Los DTOs se relacionan con las entidades de dominio. `ConexionBD` es utilizada por la capa de acceso a datos. La interfaz `Observador` soporta el patrón Observer implementado en la lógica de negocio.

## 5. Conclusión
El diagrama C4 de Nivel 4 de clases de apoyo permite comprender los componentes transversales y de soporte de GastroFlow que facilitan la integración entre la interfaz, la lógica de negocio y el acceso a datos.
