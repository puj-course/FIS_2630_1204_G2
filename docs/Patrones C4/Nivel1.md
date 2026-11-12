# Diagrama de Contexto C4 – Nivel 1

## 1. Descripción general

El diagrama de contexto C4 de **GastroFlow** presenta una visión general del sistema y su interacción con los diferentes usuarios que participan en la operación de un restaurante.

Su objetivo es identificar los actores principales, sus responsabilidades y la manera en que se relacionan con el sistema, sin profundizar en los detalles técnicos de su implementación.

GastroFlow es una aplicación de escritorio diseñada para gestionar las operaciones internas de un restaurante, centralizando funcionalidades relacionadas con la administración de mesas, el registro de pedidos, la preparación de comandas, la gestión del inventario y los procesos de caja.

## 2. Diagrama de contexto

A continuación, se presenta el diagrama de contexto C4 correspondiente al Nivel 1 de GastroFlow.

![Diagrama de Contexto C4 Nivel 1 - GastroFlow](../Imagenes/C4%20Nivel%201.jpg)

## 3. Actores del sistema

El sistema contempla cuatro actores principales, cada uno con responsabilidades específicas dentro de la operación del restaurante.

### Mesero

Se encarga de asignar mesas a los clientes y registrar los pedidos dentro del sistema, facilitando la gestión de la atención.

### Personal de cocina

Consulta las comandas registradas y actualiza el estado de preparación de los pedidos, permitiendo realizar un seguimiento de su avance.

### Cajero

Gestiona los cobros, aplica descuentos, registra los pagos y realiza el cierre de caja.

### Administrador

Administra el inventario, los productos, los ingredientes, las recetas y las configuraciones generales del sistema.

## 4. Relaciones e interacciones

Las relaciones representadas en el diagrama permiten identificar las funcionalidades a las que accede cada actor y su participación dentro del flujo operativo del restaurante.

Cada actor interactúa directamente con GastroFlow según las responsabilidades correspondientes a su rol.

De esta manera, el sistema centraliza los procesos administrativos y operativos del restaurante.

## 5. Sistemas externos

GastroFlow funciona como un sistema independiente, sin integraciones con plataformas externas.

Actualmente, el sistema no cuenta con:

- Integraciones con pasarelas de pago.
- Servicios externos de facturación electrónica.
- Conexiones con otros sistemas de gestión.

Los procesos de facturación, pagos y administración se realizan internamente dentro del restaurante.

## 6. Conclusión

El diagrama de contexto corresponde al primer nivel del modelo C4 y permite comprender el alcance general de GastroFlow, identificando los usuarios que interactúan con el sistema y sus principales responsabilidades.

Este diagrama sirve como punto de partida para la documentación de los niveles inferiores del modelo C4, en los cuales se detallan progresivamente los componentes y la estructura interna del sistema.
