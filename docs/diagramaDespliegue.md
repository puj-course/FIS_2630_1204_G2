# Diagrama de despliegue

**Issue:** HU-130 (#275)  
**Rama:** `develop`  
**Commit:** `084c099`  
**Fecha del commit:** 2026-10-15

Documenta dónde se ejecuta cada pieza. El proyecto no produce un instalable, así que el diagrama recoge el entorno de desarrollo y el de integración continua, que son los dos despliegues que existen.

Es un solo diagrama.

---

## Despliegue: entorno de desarrollo e integración continua

![Despliegue: entorno de desarrollo e integración continua](Imagenes/DiagramaDespliegue_Entorno.png)

**Por qué está.** GastroFlow no tiene empaquetado de distribución: el pom.xml no declara ningún plugin que produzca un instalable. Dibujar servidores de aplicaciones sería inventar una topología que no existe. El diagrama documenta el despliegue que sí ocurre, que es el del entorno de desarrollo y el del runner de integración continua.

**Qué muestra.** Son 6 nodos y 15 artefactos: el equipo de desarrollo con su JDK, el árbol del proyecto, la base PostgreSQL, y el repositorio en GitHub con el runner que ejecuta los pasos declarados en .github/workflows/ci.yml. Las versiones de las dependencias se leen del pom.xml.

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
