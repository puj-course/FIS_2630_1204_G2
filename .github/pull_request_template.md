## Qué cambia

<!-- Una o dos frases. Lo que hace el cambio, no lo que se intentó. -->

Closes #

## Por qué

<!-- El problema que resuelve. Si es una historia de usuario, su numero y titulo. -->

## Cómo se probó

<!-- Lo que se corrio y que salio. Si no se probó, decirlo. -->

- [ ] `mvn clean compile` termina en BUILD SUCCESS
- [ ] Si toca la base: `./scripts/setup.sh --recrear` termina sin errores
- [ ] Si toca la interfaz: se abrió la pantalla y se verificó a ojo

## Revisión del diff

- [ ] `git diff develop...HEAD` solo muestra archivos que yo toqué
- [ ] `git grep -n "^<<<<<<< \|^>>>>>>> "` no devuelve nada
- [ ] El correo de los commits es el de mi cuenta de GitHub

## Notas para quien revisa

<!-- Lo que conviene mirar con cuidado, decisiones que quedaron abiertas,
     cosas que quedaron pendientes a proposito. -->
