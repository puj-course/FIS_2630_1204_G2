# Módulo: Registro Cliente

## Descripción
Este módulo permite registrar la información básica de un cliente en el sistema
(nombre, teléfono y correo electrónico), almacenándola en la tabla `cliente` de la
base de datos.

## Historia de usuario relacionada
Registro de clientes (pendiente de número de issue)

## Estructura de archivos

| Archivo | Ubicación | Responsabilidad |
|---|---|---|
| `Cliente.java` | `src/models/RegistroCliente/` | Representa los datos de un cliente (modelo). |
| `ClienteRepository.java` | `src/repositories/RegistroCliente/` | Ejecuta el `INSERT` del cliente en la tabla `cliente`. |

## Atributos del modelo `Cliente`

| Atributo | Tipo | Descripción |
|---|---|---|
| `id` | `int` | Identificador único, generado automáticamente por la base de datos. |
| `nombre` | `String` | Nombre completo del cliente. |
| `telefono` | `String` | Número de teléfono/celular del cliente. |
| `correo` | `String` | Correo electrónico del cliente (opcional). |

## Tabla de base de datos: `cliente`

⚠️ **Provisional** — esta tabla aún no ha sido definida oficialmente por el equipo de
Database Engineer. La estructura usada actualmente es:

```sql
CREATE TABLE cliente (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100),
    telefono VARCHAR(20),
    correo VARCHAR(100)
);
```

Esta definición se usó únicamente para pruebas locales y **debe confirmarse/ajustarse**
una vez el equipo de base de datos publique el `.ddl` oficial de esta tabla.

## Pendientes / mejoras futuras
- Confirmar con el equipo de base de datos la estructura definitiva de la tabla `cliente`.
- Validar unicidad de documento de identidad y/o correo, una vez se defina ese campo.
- Integración con los módulos de pedidos y facturación (fuera del alcance actual).

## Notas
Código desarrollado con apoyo de una LLM, sujeto a ajustes tras la publicación de la
tabla oficial `cliente` por parte del equipo de base de datos.
