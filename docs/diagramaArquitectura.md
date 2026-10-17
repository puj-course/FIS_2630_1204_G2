# Diagrama de arquitectura

**Issue:** HU-128 (#273)  
**Rama:** `develop`  
**Commit:** `084c099`  
**Fecha del commit:** 2026-10-15

Documenta la organización en capas del sistema y comprueba que el código de la rama responde a la decisión de arquitectura registrada en architecture.md.

Es un solo diagrama.

---

## Arquitectura en capas del sistema

![Arquitectura en capas del sistema](Imagenes/DiagramaArquitectura_Capas.png)

**Por qué está.** La decisión de arquitectura registrada en docs/architecture.md es JavaFX con JDBC y sin Spring. Este diagrama es la comprobación de que el código que hay en la rama responde a esa decisión, y es el que resuelve la HU-128.

**Qué muestra.** Son 20 elementos y 9 dependencias entre capas. Están los paquetes de src/main/java agrupados en presentación, negocio, datos e infraestructura, los recursos de src/main/resources y PostgreSQL. Los tres DAO están en la capa de datos porque ejecutan SQL, no en la de negocio. El diagrama marca además los saltos de capa que el código comete, con su número real de ocurrencias.

---


## Cómo se construyeron

Estos diagramas no se dibujaron a mano. Los genera un conjunto de scripts que
leen el código fuente de la rama y producen el `.puml`, y se comprueban contra
ese mismo código con verificadores independientes que vuelven a leerlo con otro
método. Si el código cambia y el diagrama deja de ser cierto, la comprobación
falla.

| Qué | Dónde |
|---|---|
| Rama | `develop` |
| Commit | `084c099` (084c099423b4bdd827894e4907aca8b943b1a963) |
| Fecha del commit | 2026-10-15 |
| Fuente de los diagramas | PlantUML, en `docs/diagramas-fuente/` |
| Herramienta que los genera | `docs/diagramas-fuente/herramientas/` |
| Imágenes | PNG a dpi 96, en `docs/Imagenes/` |

Para regenerarlos hace falta el clon de la rama y una sola orden:

```bash
cd docs/diagramas-fuente/herramientas
./rehacer.sh /ruta/al/clon/de/FIS_2630_1204_G2
```

Esa orden vuelve a leer el código, regenera los `.puml`, los renderiza y corre
todas las comprobaciones. Necesita Python 3, Java y PlantUML. Los archivos
intermedios los deja dentro de esa misma carpeta.
