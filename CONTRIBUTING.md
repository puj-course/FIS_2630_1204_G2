# Cómo trabajamos en GastroFlow

Reglas de colaboración del equipo. El objetivo es simple: que `develop` compile
siempre y que nadie pierda trabajo por un merge mal hecho.

---

## 0. Acuerdo del equipo

Estas reglas fueron acordadas por el equipo antes de escribirse.

**Integrantes participantes**

- Gabriel Quiroga
- Samuel Zeudec Malaver León
- Nassin Suz
- Julián Parra
- Mariana N. V.

**Fecha del acuerdo:** lunes 5 de octubre de 2026

Cambiar cualquier regla de este documento requiere acuerdo del equipo, no un PR
de una sola persona.

---

## 1. Ramas

| Rama | Para qué | Quién escribe en ella |
|---|---|---|
| `main` | Versión entregada al profesor | Nadie directamente; entra por PR desde `develop` |
| `develop` | Integración del equipo | Nadie directamente; entra por PR |
| `features-<nombre>` | Trabajo personal de cada integrante | Su dueño |
| `fix/<tema>` | Corrección puntual y acotada | Quien la abre |

Nombres en minúscula y sin tildes: `fix/develop-compila`, no `fix/Develop Compila`.

### Antes de empezar una Historia de Usuario

Traer siempre los cambios de `develop` primero:

```bash
git checkout develop
git pull origin develop
git checkout features-<nombre>
git merge develop
```

### Antes de abrir el Pull Request

Volver a actualizarse con `develop`. Entre que se empezó la historia y que se
termina, `develop` se movió:

```bash
git checkout develop
git pull origin develop
git checkout features-<nombre>
git merge develop
```

Si el merge trae conflictos, se resuelven aquí, no dentro del PR.

### Rama sin trabajo pendiente

Si la rama personal no tiene commits propios sin subir —todo lo suyo ya está en
`develop`— **no se hace merge**: se reinicia desde `develop`.

```bash
git log origin/features-<nombre>..features-<nombre>   # vacio = nada pendiente
git fetch origin
git reset --hard origin/develop
```

Un merge de una rama vieja sin trabajo propio no aporta nada y sí arrastra
archivos viejos encima de lo que otros ya corrigieron. Así se perdieron archivos
en este proyecto.

---

## 2. Commits

Un commit es un cambio con sentido propio. No se mezclan en el mismo commit una
historia de usuario y el arreglo de un error que no tiene que ver.

Formato del mensaje:

```
HU-NN: que cambia, en presente y en una linea

Por que cambia, si no es evidente. Que se probo y como.
```

Ejemplo:

```
HU-115: agrega los DDL de las tablas que faltaban

Las consultas de PlatoInsumoRepository y ReglaDescuentoRepository
apuntaban a tablas sin definicion. Probado corriendo
scripts/setup.sh contra PostgreSQL 16 en limpio.
```

Antes de hacer commit, revisar que el correo configurado sea el de la cuenta de
GitHub. Si no, el commit no aparece como contribución de nadie:

```bash
git config user.email
```

---

## 3. Pruebas

Cada Historia de Usuario nueva incluye **al menos una prueba automatizada**, en
`src/test/java/`, que ejercite lo que la historia agrega.

Si la historia no toca código ejecutable —documentación, configuración del
repositorio, scripts— se dice en el PR por qué no aplica, en lugar de omitirlo
en silencio.

Las pruebas que necesitan base de datos se saltan solas cuando no hay una
disponible, en vez de fallar: así quien compile sin base no ve el build en rojo.
Ver `MesaModuloFuncionalTest` como referencia.

---

## 4. Pull requests

Todo entra a `develop` por PR. Nadie empuja directo, ni siquiera un cambio de una
línea.

Antes de abrirlo:

1. Actualizarse con `develop` (sección 1).
2. Compilar: `mvn clean compile`. Si no compila, no se abre el PR.
3. Correr las pruebas: `mvn test`.
4. Si el cambio toca la base de datos, correr `./scripts/setup.sh --recrear` y
   confirmar que la instalación desde cero termina sin errores.
5. Revisar el propio diff: `git diff develop...HEAD`. Si aparecen archivos que
   uno no tocó, hay un merge mal resuelto. Resolverlo antes, no en el PR.
6. Buscar marcadores de conflicto olvidados:
   ```bash
   git grep -n "^<<<<<<< \|^>>>>>>> "
   ```
   Ya pasó una vez: `categorias.ddl` llegó a `develop` con los marcadores
   adentro y eso rompió la creación de la base para todo el equipo.

### Revisión

Todo PR necesita **al menos una aprobación de alguien distinto al autor**. Uno no
aprueba su propio PR.

Quien revisa mira que compile, que el diff no arrastre cambios ajenos y que lo
que dice el PR sea lo que hace el código.

El cuerpo del PR lleva `Closes #NN` con el número del issue, para que se cierre
solo al fusionar.

---

## 5. Conflictos y archivos eliminados

**Conflicto sobre el archivo de otro integrante.** Quien resuelve el conflicto
consulta primero con el autor del archivo. No se decide por cuenta propia cuál de
las dos versiones queda: quien escribió ese código sabe qué estaba haciendo y uno
no. Basta un mensaje al grupo antes de resolver.

**Merge que elimina archivos.** Si el PR muestra archivos eliminados, se revisa
explícitamente antes de aprobar: se confirma con el equipo que cada borrado es
intencional. Un merge de una rama desactualizada puede proponer borrar decenas de
archivos que otros agregaron después, y en el diff eso se ve igual que un borrado
a propósito.

Para ver solo los borrados de un PR:

```bash
git diff --diff-filter=D --name-only develop...HEAD
```

---

## 6. Describir lo que se hizo

La descripción del PR dice lo que el cambio hace de verdad. Si el diff toca
autenticación, la descripción menciona la autenticación aunque el trabajo visible
haya sido la pantalla. Un PR que suena más pequeño de lo que es hace que el
revisor no mire lo que más necesita revisión.

---

## 7. Protección de `develop`

Configuración que debe quedar aplicada en GitHub, en Settings → Branches → Add
branch ruleset, sobre `develop`:

- [ ] Require a pull request before merging
- [ ] Require approvals: **1**
- [ ] Dismiss stale pull request approvals when new commits are pushed
- [ ] Require conversation resolution before merging
- [ ] Block force pushes
- [ ] Restrict deletions

Las tres primeras son las que exige la historia: aprobación mínima de uno y
prohibición de empujar directo sin PR.

Esto lo aplica quien tenga permisos de administrador sobre
`puj-course/FIS_2630_1204_G2`. Mientras no esté aplicado, las reglas de este
documento son un acuerdo del equipo y nada impide saltárselas técnicamente.

---

## 8. Base de datos

Los cambios de esquema no se hacen a mano sobre la base de cada uno. Van en
archivos versionados:

- Tabla nueva → un archivo en `db/ddl/`, y agregarla a la lista `TABLAS` de
  `scripts/setup.sh` en la posición que le corresponde según sus llaves foráneas.
- Cambio sobre una tabla que ya existe → una migración nueva en `db/migrations/`,
  numerada en secuencia, **idempotente** (`IF NOT EXISTS`, `WHERE NOT EXISTS`):
  tiene que poder correrse dos veces sin fallar ni duplicar datos.

Detalles en `db/ddl/README.md`.

---

## 9. Antes de pedir ayuda

Si `develop` no compila, antes de tocar nada:

```bash
git fetch origin
git log --oneline -5 origin/develop
mvn clean compile
```

El error de Maven dice el archivo y la línea. Casi siempre es un merge mal
resuelto en un commit reciente, y lo arregla quien lo hizo, no quien lo encontró.
