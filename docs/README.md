# Documentación técnica de GastroFlow

Esta carpeta reúne la documentación técnica de la aplicación. La descripción general del producto y del equipo se encuentra en el `README.md` de la raíz del repositorio.

| Documento | Contenido |
|---|---|
| [architecture.md](architecture.md) | Arquitectura JavaFX + JDBC + PostgreSQL, capas y navegación. |
| [aplicacion.md](aplicacion.md) | Módulos, vistas y procesos principales. |
| [database.md](database.md) | Organización del esquema, relaciones y funciones almacenadas. |
| [data-access.md](data-access.md) | Conexión JDBC, DAO, repositorios y transacciones. |
| [setup.md](setup.md) | Requisitos, instalación, configuración, ejecución y pruebas. |

Los documentos se elaboraron contrastando el código fuente, `pom.xml`, los DDL, las migraciones y los scripts incluidos en la versión revisada del repositorio. Cuando existe una diferencia entre un documento antiguo y los archivos ejecutables, se señala la discrepancia en lugar de asumir que el documento anterior está actualizado.
