# Diagrama de Código C4 – Nivel 4 (Interfaz de usuario)

## 1. Descripción general
El diagrama de código C4 de GastroFlow correspondiente al módulo de interfaz de usuario muestra las clases y controladores responsables de la capa de presentación.

Este nivel detalla cómo se organizan las pantallas, los controladores y las relaciones entre ellos para gestionar la interacción con los diferentes roles del sistema.

## 2. Diagrama de código
A continuación, se presenta el diagrama C4 de Nivel 4 – Interfaz de usuario de GastroFlow.

<img width="5348" height="4178" alt="C4 Nivel 4 - Interfaz de usuario" src="https://github.com/user-attachments/assets/5254d174-dc51-4d76-b383-7a6758728bc5" />

## 3. Organización de las clases
Las clases se agrupan principalmente en:
- **Controladores de pantalla:** `LoginController`, `MenuPrincipalController`, `MeseroController`, `CocinaController`, `CajeroController`, `AdministradorController`, entre otros.
- **Componentes de UI y utilidades de interfaz.**
- **Relaciones con servicios y modelos de las capas inferiores.**

## 4. Relaciones entre clases
Los controladores dependen de los servicios de lógica de negocio y de las clases de entidad/DTO. Se observa una estructura centrada en controladores que orquestan la interfaz gráfica y delegan la lógica a capas inferiores.

## 5. Conclusión
El diagrama C4 de Nivel 4 de la interfaz de usuario permite comprender la estructura de la capa de presentación de GastroFlow, sus controladores y las dependencias hacia la lógica de negocio.
