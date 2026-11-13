#!/usr/bin/env bash
#
# GastroFlow - construye el paquete de entrega.
#
# Compila la aplicacion, reune sus bibliotecas y arma un zip listo para
# entregar, con las instrucciones de como ponerlo a andar.
#
# Lo usa el pipeline de CD (.github/workflows/cd.yml), y tambien se puede
# correr a mano para revisar que va a salir antes de publicar una version.
#
# Uso:
#   ./scripts/deploy.sh                 arma el paquete con la version del pom
#   ./scripts/deploy.sh v1.0.42         arma el paquete con la version indicada
#
# Deja el zip en la raiz del proyecto y escribe su nombre en la salida.

set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$BASE_DIR"

# ---------------------------------------------------------------- version --

if [ $# -ge 1 ]; then
    VERSION="$1"
else
    BASE=$(mvn --batch-mode -q help:evaluate \
           -Dexpression=project.version -DforceStdout | tr -d '[:space:]')
    VERSION="v${BASE%-SNAPSHOT}"
fi

CARPETA="gastroflow-${VERSION}"
echo ">> armando el paquete $CARPETA"

# ------------------------------------------------------------- construir --

echo ">> compilando"
mvn --batch-mode -DskipTests clean package

echo ">> reuniendo las bibliotecas"
mvn --batch-mode dependency:copy-dependencies \
    -DoutputDirectory=target/lib \
    -DincludeScope=runtime

# ---------------------------------------------------------------- armar --

rm -rf "$CARPETA" "${CARPETA}.zip"
mkdir -p "$CARPETA/lib"

cp target/*.jar "$CARPETA/" 2>/dev/null || {
    echo "error: no se genero ningun jar en target/" >&2
    exit 1
}
cp -r target/lib/. "$CARPETA/lib/"

if [ -d src/main/resources ]; then
    cp -r src/main/resources "$CARPETA/recursos"
fi

if [ -f conf/database.properties.example ]; then
    cp conf/database.properties.example "$CARPETA/"
fi

cat > "$CARPETA/COMO-EJECUTAR.txt" <<'TEXTO'
GastroFlow
==========

1. Base de datos

   Hace falta PostgreSQL 16 con el esquema ya creado. Desde el repositorio:

       ./scripts/setup.sh --con-datos-prueba

2. Datos de conexión

   Copie database.properties.example a conf/database.properties y escriba su
   contraseña. Como alternativa, defina las variables de entorno PGHOST,
   PGPORT, PGDATABASE, PGUSER y PGPASSWORD.

3. Ejecutar

       java -cp "VisualMesas.jar:lib/*" Main

   En Windows el separador del classpath es punto y coma:

       java -cp "VisualMesas.jar;lib\*" Main

Nota sobre JavaFX
-----------------
Las bibliotecas de JavaFX que vienen en lib/ son las del sistema operativo
donde se construyó el paquete. Si lo va a ejecutar en otro sistema, constrúyalo
desde el repositorio con "mvn javafx:run", que descarga las que corresponden a
su equipo.
TEXTO

zip -r "${CARPETA}.zip" "$CARPETA" > /dev/null
rm -rf "$CARPETA"

echo ">> listo"
ls -lh "${CARPETA}.zip"
echo "${CARPETA}.zip"
