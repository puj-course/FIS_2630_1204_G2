# Diagrama de Código C4 – Nivel 4 (Modelo de datos)

## 1. Descripción general
El diagrama de código C4 de GastroFlow correspondiente al modelo de datos muestra las entidades del dominio y sus atributos.

Este nivel detalla las clases que representan los conceptos principales del restaurante (mesas, pedidos, productos, inventario, reservas, etc.) y cómo se relacionan mediante identificadores.

## 2. Diagrama de código
A continuación, se presenta el diagrama C4 de Nivel 4 – Modelo de datos de GastroFlow.

<img width="1890" height="1852" alt="C4 Nivel 4 - Modelo de datos" src="https://github.com/user-attachments/assets/d345a0d0-3eeb-4f23-9a70-87a33ed8265e" />


## 3. Organización de las clases
Las entidades principales incluyen:
- `Ingrediente`, `Plato`, `Producto`, `Pedido`, `Mesa`, `Cliente`, `Reserva`
- `MovimientoInventario`, `EntradaInventario`, `AlertaInventario`
- `DetallePedido`, `DetallePedidoAdicional`, `ReglaDescuento`
- `AsignacionMesa`, `HistorialEstadoMesa`, `ListaEspera`, `Zona`, `EstadoMesa`, etc.

Las entidades se relacionan entre sí principalmente mediante identificadores (IDs).

## 4. Relaciones entre clases
Aunque el diagrama enfatiza que las entidades se relacionan mediante identificadores, se pueden observar asociaciones lógicas entre pedidos, detalles, productos, mesas, reservas e inventario.

## 5. Conclusión
El diagrama C4 de Nivel 4 del modelo de datos permite comprender el dominio de GastroFlow, sus entidades principales y la forma en que se estructuran los datos del sistema.
