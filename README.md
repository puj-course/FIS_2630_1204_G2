# GastroFlow

## Descripción
GastroFlow es un sistema de software orientado a la gestión integral de restaurantes. Su objetivo es centralizar en una sola plataforma procesos como el control de inventario, gestión de mesas, registro de pedidos, envío de comandas a cocina, administración de productos, recetas, usuarios y generación de reportes.

El problema que busca solucionar es la falta de organización y trazabilidad que puede presentarse cuando estos procesos se manejan de forma manual o mediante herramientas separadas. Esto puede generar errores en los pedidos, desactualización del inventario, pérdida de información, dificultades en la comunicación entre meseros y cocina, y poca visibilidad sobre la operación del restaurante. El proyecto se plantea como una solución modular y escalable, de manera que puedan incorporarse nuevas funcionalidades a futuro sin afectar la estructura principal del sistema. GastroFlow busca mejorar la eficiencia operativa, facilitar el control de los recursos y proporcionar información más clara para apoyar la gestión y la toma de decisiones dentro del restaurante.


---

## Equipo del Proyecto
| Nombre        | Rol                   | GitHub / Perfil |
|--------------|-----------------------|-----------------|
| Mariana Niño | Scrum Master          | github.com/mariananvv16 |
| Gabriel Quiroga | Product Owner         | github.com/Quirogaaaa |
| Mariana Niño | Sprint Planner        | github.com/mariananvv16 |
| Samuel Zeudec | Configuration Manager | github.com/szml-PUJ |
| Julian Parra | QA Lead               | github.com/JulianLeal12 |
| Nassin Suz | DevOps Engineer       | github.com/zeuznnss |

---

## Tecnologías Utilizadas
- **Frontend:** JavaFX
- **Backend:** Java – Spring Boot
- **Base de Datos:** PostgreSQL
- **IA / Data Science:** Python, Pandas, Scikit-learn
- **DevOps:** GitHub Actions, Docker
- **Control de versiones:** Git

---

## Estructura del Proyecto
```text
project-name/
├── app/
│   ├── index.js
│   │   └── Punto de entrada principal de la aplicación.
│   ├── package.json
│   │   └── Define las dependencias, scripts y configuración del proyecto.
│   ├── routes/
│   │   ├── index.js
│   │   └── Define las rutas o endpoints principales de la aplicación.
│   ├── controllers/
│   │   ├── userController.js
│   │   └── Contiene la lógica encargada de procesar las solicitudes.
│   └── services/
│       ├── userService.js
│       └── Contiene la lógica de negocio y servicios reutilizables.
│
├── conf/
│   ├── config.json
│   │   └── Contiene parámetros generales de configuración.
│   ├── database.js
│   │   └── Configura la conexión con la base de datos.
│   └── environment.example
│       └── Ejemplo de las variables de entorno necesarias para ejecutar el proyecto.
│
├── docs/
│   ├── architecture.md
│   │   └── Describe la arquitectura general del sistema.
│   ├── api.md
│   │   └── Documenta los endpoints, parámetros y respuestas de la API.
│   ├── installation.md
│   │   └── Explica cómo instalar y configurar el proyecto.
│   └── user_guide.md
│       └── Guía básica para el uso de la aplicación.
│
├── scripts/
│   ├── setup.sh
│   │   └── Automatiza la instalación y configuración inicial del proyecto.
│   ├── start.sh
│   │   └── Permite iniciar la aplicación.
│   ├── test.sh
│   │   └── Ejecuta las pruebas automatizadas.
│   └── deploy.sh
│       └── Automatiza tareas relacionadas con el despliegue.
│
├── src/
│   ├── models/
│   │   ├── user.js
│   │   └── Define las estructuras o modelos de datos del sistema.
│   ├── utils/
│   │   ├── helpers.js
│   │   └── Contiene funciones auxiliares reutilizables.
│   ├── middleware/
│   │   ├── auth.js
│   │   └── Contiene funciones que se ejecutan antes o después de una solicitud.
│   └── tests/
│       ├── user.test.js
│       └── Contiene pruebas unitarias o de integración del proyecto.
│
├── temp/
│   ├── .gitkeep
│   │   └── Permite conservar la carpeta vacía dentro del repositorio.
│   ├── example.tmp
│   │   └── Ejemplo de archivo temporal generado durante la ejecución.
│   └── uploads/
│       └── Carpeta destinada a almacenar archivos temporales cargados por usuarios.
│
├── BOILERPLATE_template.md
│   └── Documento que explica la estructura base y cómo utilizar este boilerplate.
│
├── CONTRIBUTING.md
│   └── Define las normas y recomendaciones para contribuir al proyecto.
│
├── LICENSE
│   └── Especifica la licencia bajo la cual se distribuye el proyecto.
│
├── README.md
│   └── Documento principal con la descripción, instalación, uso y características del proyecto.
│
├── .gitignore
│   └── Define los archivos y carpetas que Git no debe versionar.
│
└── .env.example
    └── Plantilla de las variables de entorno necesarias para ejecutar la aplicación.
```

---

## Instalación y Ejecución
**Requisitos**
- Docker y Docker Compose
- Git
- Java 17+
- Python 3.10+

## Clonar el repositorio
```text
git clone https://github.com/puj-course/FIS_2630_1204_G2.git
cd FIS_2630_1204_G2
```

## Ejecución con Docker
```text
docker-compose up --build
```

## Ejecución de pruebas
```text
docker-compose run backend mvn test
docker-compose run ai-model pytest
```

---

## Contexto Académico
- **Asignatura:** Fundamentos de Ingeniería de Software
- **Docente:** Luis Gabriel Moreno Sandoval, PhD
- **Contacto:** morenoluis@javeriana.edu.co

---

## Contacto

**Equipo de desarrollo:**

**Gabriel Quiroga**  
Estudiante de Ingenieria en Sistemas, Pontificia Universidad Javeriana  
📧 quirogarg@javeriana.edu.co  

**Julian Parra**  
Estudiante de Ingeniería en Sistemas, Pontificia Universidad Javeriana  
📧 julians_parra@javeriana.edu.co

**Samuel Zeudec**  
Estudiante de Ingeniería en Sistemas, Pontificia Universidad Javeriana  
📧 malaverl-sz@javeriana.edu.co 

**Nassin Suz**  
Estudiante de Ingeniería en Sistemas, Pontificia Universidad Javeriana  
📧 suz.nassinn@javeriana.edu.co

**Mariana Niño**  
Estudiante de Ingeniería en Sistemas, Pontificia Universidad Javeriana  
📧 ninov.mariana@javeriana.edu.co

--- 

## Licencia
Proyecto desarrollado con fines académicos.
* Gabriel Quiroga: Product Owner - https://github.com/Quirogaaaa
* Ana Torres: Backend Developer - https://github.com/anatorres

