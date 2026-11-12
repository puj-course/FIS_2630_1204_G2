# Diagrama de Contenedores C4 – Nivel 2

<img width="1540" height="616" alt="C4 Nivel 2" src="https://github.com/user-attachments/assets/5a127d34-282f-425d-976f-e89e152c5a9c" />


## 1. Descripción general
El diagrama de contenedores C4 de GastroFlow muestra la vista de alto nivel del sistema, identificando los principales contenedores de software, los actores que interactúan con él y las relaciones tecnológicas entre ellos.

Este nivel permite comprender cómo se estructura la aplicación de escritorio y cómo se comunica con la base de datos para soportar las operaciones del restaurante.

## 2. Diagrama de contenedores
A continuación, se presenta el diagrama C4 de Nivel 2 correspondiente a GastroFlow.

## 3. Organización de los contenedores
Los elementos se agrupan de la siguiente manera:
- **Actores (Personas):** Mesero, Personal de cocina, Cajero y Administrador.
- **Aplicación de escritorio:** Contenedor principal desarrollado en Java 21 + JavaFX 21.0.5 + Maven. Presenta la interfaz gráfica a los cuatro roles y contiene la lógica de negocio.
- **Base de datos:** PostgreSQL (driver JDBC 42.7.5) que almacena usuarios, mesas, reservas, pedidos, productos, ingredientes, recetas e inventario.

## 4. Relaciones entre contenedores
- Los cuatro roles interactúan directamente con la aplicación de escritorio mediante las operaciones propias de su función.
- La aplicación de escritorio se comunica con la base de datos mediante JDBC, leyendo y escribiendo datos e invocando funciones almacenadas.
- El sistema completo se denomina GastroFlow.

## 5. Conclusión
El diagrama C4 de Nivel 2 permite visualizar la arquitectura de contenedores de GastroFlow, facilitando la comprensión de los actores, la aplicación de escritorio y la base de datos, así como las interacciones principales del sistema.
