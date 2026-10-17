# Diagramas de estados

**Issue:** HU-129 (#274)  
**Rama:** `develop`  
**Commit:** `084c099`  
**Fecha del commit:** 2026-10-15

Documentan los estados por los que pasa un pedido y una mesa, y qué transiciones son válidas entre ellos. Cada flecha lleva un número y el detalle está en una tabla, para que ninguna etiqueta quede flotando entre dos líneas.

Son 2 diagramas.

---

## Estados del pedido

![Estados del pedido](Imagenes/DiagramaEstados_Pedido.png)

**Por qué está.** La HU-129 pide saber qué transiciones son válidas para un pedido. El estado vive en el enum EstadoPedido y se cambia desde varios sitios, así que sin un diagrama no hay forma de responder esa pregunta sin leerse todo el código.

**Qué muestra.** 6 estados y 6 transiciones numeradas. Los cuatro estados no cerrados van dentro del bloque «activo», y de ese bloque salen las dos transiciones de cierre: cerrarPedido y cancelarPedido no miran en qué estado está el pedido, solo que no sea uno de los dos finales. La tabla da, por cada transición, el origen, el destino, el método que la ejecuta y la condición que debe cumplirse. ASIGNADO_MESA va en gris porque ningún código lo asigna, y las tres flechas azules escriben con pedidoRepository.save() sin pasar por PedidoService, saltándose la guarda ESTADOS_YA_CERRADOS.

---

## Estados de la mesa

![Estados de la mesa](Imagenes/DiagramaEstados_Mesa.png)

**Por qué está.** La otra mitad de la HU-129. El estado de la mesa no es un enum de Java sino una fila de la tabla estados_mesa, y se cambia desde cinco sitios distintos, dos de ellos saltándose la capa de servicio.

**Qué muestra.** 7 estados y 13 transiciones numeradas. Ninguna transición consulta el estado actual de la mesa salvo asignarComensales, que lo exige en su propio SQL, y por eso es la única flecha que sale de un estado concreto; las demás salen del bloque «cualquier estado del catálogo». MANTENIMIENTO va en gris porque está en el catálogo y ningún código lo asigna. LIBRE va en rojo porque el código lo pide y el catálogo no lo tiene. Las flechas azules escriben sin pasar por MesaService y por tanto no dejan fila en historial_estado_mesa.

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
