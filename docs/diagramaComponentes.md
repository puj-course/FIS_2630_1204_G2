# Diagramas de componentes

**Issue:** Documentación de diseño y consolidación del modelo de datos — trabajo previo, no corresponde a ninguna de las tres HU de esta semana  
**Rama:** `develop`  
**Commit:** `084c099`  
**Fecha del commit:** 2026-10-15

Documentan los 49 componentes del sistema y las dependencias entre ellos. Las interfaces provistas no son nombres inventados: cada una se dedujo de las llamadas que de verdad cruzan el límite entre dos componentes, medidas sobre el código.

Son 8 diagramas.

---

## Vista general

![Vista general](Imagenes/DiagramaComponentes_General.png)

**Por qué está.** Es el punto de entrada al resto de la serie. Un solo diagrama con los 49 componentes resultaba ilegible, así que esta lámina da la vista de conjunto y las siete siguientes la desarrollan por capas.

**Qué muestra.** Muestra los cinco grupos de componentes (interfaz, negocio, datos, conexión y apoyo) y las dependencias entre grupos. Las interfaces provistas no llevan nombres inventados: cada una se dedujo de las llamadas que de verdad cruzan el límite entre un grupo y otro, medidas sobre el código.

---

## Capa de interfaz de usuario

![Capa de interfaz de usuario](Imagenes/DiagramaComponentes_UI.png)

**Por qué está.** La capa de presentación es la que consume más interfaces y la que hay que revisar cuando se agrega una pantalla.

**Qué muestra.** Son 20 componentes de interfaz y 14 interfaces consumidas, con 17 conexiones. Cada componente agrupa su .fxml, su hoja de estilos y su controlador. Las hojas CSS aparecen cargadas por Main, que es donde el código las carga de verdad.

---

## Capa de lógica de negocio

![Capa de lógica de negocio](Imagenes/DiagramaComponentes_Negocio.png)

**Por qué está.** Es la capa donde se decide si una operación es válida, y por tanto donde más interesa ver con precisión qué depende de qué.

**Qué muestra.** Son 30 componentes de negocio y 23 interfaces, con 28 conexiones. Incluye SesionUsuario, que es el Singleton del proyecto, y marca PagoService como excluida de la compilación en el pom.xml.

---

## Capa de datos y tablas propias

![Capa de datos y tablas propias](Imagenes/DiagramaComponentes_Datos.png)

**Por qué está.** Sin ORM, la relación entre repositorio y tabla solo existe dentro de las cadenas de SQL. Este diagrama la hace explícita.

**Qué muestra.** Son 27 componentes de datos y 16 tablas, con 34 conexiones. Cada flecha sale de una consulta real leída del código. Aquí aparecen también los tres DAO, que ejecutan SQL igual que los repositorios.

---

## Tablas compartidas

![Tablas compartidas](Imagenes/DiagramaComponentes_Datos_Compartidos.png)

**Por qué está.** Una tabla escrita desde varios sitios es un riesgo de consistencia. Separarlas en su propia lámina es lo que permite verlas.

**Qué muestra.** Son 11 tablas que más de un componente consulta o modifica, con 30 conexiones. Entre ellas movimientos_inventario, escrita desde tres repositorios distintos.

---

## Artefactos: qué archivo contiene qué

![Artefactos: qué archivo contiene qué](Imagenes/DiagramaComponentes_Artefactos.png)

**Por qué está.** Las láminas anteriores hablan de componentes lógicos. Esta baja al archivo concreto, que es lo que hace falta para localizar el código.

**Qué muestra.** Son 24 artefactos y 18 relaciones: los paquetes de código fuente, los .fxml, las hojas de estilo y los recursos, con el componente lógico al que pertenece cada uno.

---

## Infraestructura transversal

![Infraestructura transversal](Imagenes/DiagramaComponentes_Infraestructura.png)

**Por qué está.** Hay tres piezas que usa medio sistema y que, dibujadas en las otras láminas, generaban líneas cruzando el diagrama entero.

**Qué muestra.** ConexionBD, SesionUsuario y TextoBusqueda con sus 31 consumidores. Sacarlas aquí es lo que permitió que las láminas por capa quedaran legibles.

---

## Código Spring que no compila

![Código Spring que no compila](Imagenes/DiagramaComponentes_Spring_Sin_Compilar.png)

**Por qué está.** El repositorio conserva seis clases que importan Spring o Jakarta sin que el pom.xml declare esas dependencias. No forman parte del sistema que se ejecuta, pero están en el código, y documentarlas evita que alguien las dé por funcionales.

**Qué muestra.** Son 12 artefactos. Muestra exactamente las clases que el maven-compiler-plugin excluye y por qué, junto con los archivos de configuración y los scripts que tampoco participan en la compilación.

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
