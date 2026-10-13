#!/usr/bin/env bash
#
# GastroFlow - instalacion de la base de datos desde cero.
#
# Crea la base, corre los DDL en orden de dependencias, las funciones almacenadas,
# las migraciones y, si se pide, los datos de prueba.
#
# Uso:
#   ./scripts/setup.sh                        base "gastroflow", solo estructura
#   ./scripts/setup.sh --con-datos-prueba     ademas siembra catalogos y datos de ejemplo
#   ./scripts/setup.sh --recrear              borra la base si ya existe y la vuelve a crear
#
# Se puede correr dos veces: sobre una base que ya existe completa lo que falte,
# no duplica datos y no falla. Para empezar de cero, --recrear.
#   ./scripts/setup.sh --db mi_base           usa otro nombre de base
#   ./scripts/setup.sh --help
#
# Conexion: se toma de las variables de entorno de PostgreSQL, las mismas que usa
# psql. Si no estan definidas, se usan los valores de abajo.
#   PGHOST (localhost)  PGPORT (5432)  PGUSER (postgres)  PGPASSWORD
#
# En Windows se corre desde Git Bash, que ya viene instalado con Git.
#
# El script se detiene en el primer error. Una base a medias es peor que ninguna.

set -euo pipefail

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DB_DIR="$BASE_DIR/db"

DB_NOMBRE="gastroflow"
CON_DATOS=0
RECREAR=0

# ---------------------------------------------------------------- argumentos --

while [ $# -gt 0 ]; do
    case "$1" in
        --db)
            [ $# -ge 2 ] || { echo "Falta el nombre despues de --db" >&2; exit 2; }
            DB_NOMBRE="$2"; shift 2 ;;
        --con-datos-prueba) CON_DATOS=1; shift ;;
        --recrear)          RECREAR=1;  shift ;;
        --help|-h)
            sed -n '2,28p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
            exit 0 ;;
        *)
            echo "Opcion desconocida: $1" >&2
            echo "Use --help para ver las opciones." >&2
            exit 2 ;;
    esac
done

export PGHOST="${PGHOST:-localhost}"
export PGPORT="${PGPORT:-5432}"
export PGUSER="${PGUSER:-postgres}"

# ------------------------------------------------------------------- salidas --

if [ -t 1 ]; then
    VERDE=$'\033[32m'; ROJO=$'\033[31m'; GRIS=$'\033[90m'; FIN=$'\033[0m'
else
    VERDE=''; ROJO=''; GRIS=''; FIN=''
fi

paso()  { printf '%s>>%s %s\n' "$GRIS" "$FIN" "$1"; }
ok()    { printf '   %sok%s   %s\n' "$VERDE" "$FIN" "$1"; }
error() { printf '%serror:%s %s\n' "$ROJO" "$FIN" "$1" >&2; }

# ------------------------------------------------------------ comprobaciones --

for cmd in psql createdb dropdb; do
    command -v "$cmd" >/dev/null 2>&1 || {
        error "no se encontro '$cmd'. Instale PostgreSQL y agregue su carpeta bin al PATH."
        exit 1
    }
done

if ! psql -d postgres -c 'SELECT 1' >/dev/null 2>&1; then
    error "no se pudo conectar a PostgreSQL en $PGHOST:$PGPORT como '$PGUSER'."
    echo  "       Revise que el servidor este corriendo y que PGUSER y PGPASSWORD sean correctos." >&2
    exit 1
fi

[ -d "$DB_DIR/ddl" ] || { error "no existe $DB_DIR/ddl. Corra el script desde el repositorio."; exit 1; }

# --------------------------------------------------------------- aplicar sql --

# Corre un archivo .sql o .ddl contra la base. ON_ERROR_STOP hace que psql
# devuelva un codigo distinto de cero ante cualquier error, que es lo que
# convierte un fallo silencioso en un fallo visible.
aplicar() {
    local archivo="$1"
    local nombre="${archivo#$DB_DIR/}"

    [ -f "$archivo" ] || { error "falta el archivo $nombre"; exit 1; }

    if ! salida=$(psql -q -v ON_ERROR_STOP=1 -d "$DB_NOMBRE" -f "$archivo" 2>&1); then
        error "fallo $nombre"
        printf '%s\n' "$salida" | sed 's/^/       /' >&2
        exit 1
    fi
    ok "$nombre"
}

# Crea la tabla solo si no esta. Los .ddl usan CREATE TABLE a secas, asi que
# correrlos sobre una base ya armada fallaria; saltarlos permite volver a correr
# el script para completar lo que falte sin tocar lo que ya existe.
crear_tabla_si_falta() {
    local tabla="$1"

    hay=$(psql -At -d "$DB_NOMBRE" -c \
          "SELECT 1 FROM information_schema.tables
            WHERE table_schema = 'public' AND table_name = '$tabla'" 2>/dev/null || true)

    if [ "$hay" = "1" ]; then
        printf '   %s--%s   ddl/%s.ddl (ya existe)\n' "$GRIS" "$FIN" "$tabla"
    else
        aplicar "$DB_DIR/ddl/$tabla.ddl"
    fi
}

# ------------------------------------------------------------- crear la base --

existe=$(psql -At -d postgres \
         -c "SELECT 1 FROM pg_database WHERE datname = '$DB_NOMBRE'" 2>/dev/null || true)

if [ "$existe" = "1" ]; then
    if [ "$RECREAR" -eq 1 ]; then
        paso "borrando la base '$DB_NOMBRE'"
        dropdb "$DB_NOMBRE"
        createdb "$DB_NOMBRE"
        ok "base '$DB_NOMBRE' recreada"
    else
        # La base ya existe: se completa lo que falte en lugar de fallar. Correr
        # el script dos veces seguidas tiene que ser seguro.
        paso "la base '$DB_NOMBRE' ya existe: se completa lo que falte"
        echo "   (use --recrear para borrarla y empezar de cero)"
    fi
else
    paso "creando la base '$DB_NOMBRE'"
    createdb "$DB_NOMBRE"
    ok "base '$DB_NOMBRE' creada"
fi

# ---------------------------------------------------------------------- DDL --
#
# El orden importa: hay llaves foraneas entre las tablas. Esta lista es la unica
# fuente del orden; el README de db/ddl la refleja.

paso "creando las tablas"

TABLAS=(
    # catalogos, sin dependencias
    roles tipos_documento zonas estados_mesa unidades_medida categorias
    # dependen de los catalogos
    usuarios                  # roles, tipos_documento
    clientes                  # tipos_documento, usuarios
    mesas                     # zonas, estados_mesa
    ingredientes              # unidades_medida
    productos                 # categorias
    precios_producto          # productos
    pedidos                   # clientes, mesas, usuarios
    detalle_pedido            # pedidos, productos
    movimientos_inventario    # productos, ingredientes, usuarios
    alerta_inventario         # ingredientes, productos
    pagos                     # pedidos, usuarios
    historial_estado_mesa     # mesas, estados_mesa
    nota_mesa                 # mesas
    reservas                  # mesas
    # HU-115
    adicional
    asignacion_mesa           # mesas, usuarios
    detalle_pedido_adicional  # detalle_pedido, adicional
    plato_insumo              # productos, ingredientes
    regla_descuento
)

for t in "${TABLAS[@]}"; do
    crear_tabla_si_falta "$t"
done

# --------------------------------------------------------------- funciones --
#
# db-hu082-login.sql queda fuera a proposito: inserta en una tabla "usuario" que
# creaba Hibernate cuando el proyecto iba a usar Spring. Con la arquitectura JDBC
# esa tabla no existe. Los usuarios de acceso los siembra la migracion 008.

# Las cinco funciones se declaran con CREATE OR REPLACE, asi que volver a
# aplicarlas sobre una base ya armada es seguro.
paso "creando las funciones almacenadas"
aplicar "$DB_DIR/functions/hu28_disponibilidad.sql"

# ------------------------------------------------------------- migraciones --
#
# Todas son idempotentes. En una base recien creada varias no cambian nada,
# porque el DDL ya trae la columna; se corren igual para que el resultado sea el
# mismo que en una base que venia de antes.

paso "aplicando las migraciones"
for m in "$DB_DIR"/migrations/[0-9]*.sql; do
    [ -e "$m" ] || break
    aplicar "$m"
done

# ----------------------------------------------------------- datos de prueba --

if [ "$CON_DATOS" -eq 1 ]; then
    paso "sembrando catalogos y datos de prueba"

    SEMILLAS=(
        generar_roles_pruebas
        generar_tipos_documento_prueba
        generar_zonas_prueba
        generar_estado_mesa_prueba
        generar_unidades_de_medida
        generar_categorias_pruebas
        generar_ingredientes_pruebas
        generar_productos_prueba    # necesita categorias
        generar_usuario_prueba      # necesita roles y tipos_documento
        generar_mesa_prueba         # necesita zonas y estados_mesa
    )

    for s in "${SEMILLAS[@]}"; do
        aplicar "$DB_DIR/functions/$s.ddl"
    done
fi

# ------------------------------------------------------------ verificacion --

paso "verificando"

tablas=$(psql -At -d "$DB_NOMBRE" -c \
    "SELECT count(*) FROM information_schema.tables
      WHERE table_schema = 'public' AND table_type = 'BASE TABLE'")

funciones=$(psql -At -d "$DB_NOMBRE" -c \
    "SELECT count(*) FROM pg_proc p
       JOIN pg_namespace n ON n.oid = p.pronamespace
      WHERE n.nspname = 'public' AND p.proname LIKE 'fn\\_%'")

esperadas=${#TABLAS[@]}

if [ "$tablas" -ne "$esperadas" ]; then
    error "se esperaban $esperadas tablas y hay $tablas."
    psql -d "$DB_NOMBRE" -c '\dt' >&2
    exit 1
fi

if [ "$funciones" -lt 4 ]; then
    error "se esperaban 4 funciones fn_* y hay $funciones."
    exit 1
fi

ok "$tablas tablas"
ok "$funciones funciones fn_*"

if [ "$CON_DATOS" -eq 1 ]; then
    psql -d "$DB_NOMBRE" -At -F' ' -c "
        SELECT 'roles', count(*) FROM roles
        UNION ALL SELECT 'zonas', count(*) FROM zonas
        UNION ALL SELECT 'estados_mesa', count(*) FROM estados_mesa
        UNION ALL SELECT 'usuarios', count(*) FROM usuarios
        UNION ALL SELECT 'mesas', count(*) FROM mesas
        UNION ALL SELECT 'productos', count(*) FROM productos
        ORDER BY 1" | while read -r t n; do ok "$t: $n filas"; done
fi

echo
printf '%sBase \x27%s\x27 lista.%s\n' "$VERDE" "$DB_NOMBRE" "$FIN"
echo
echo "Conectarse:   psql -d $DB_NOMBRE"
if [ "$CON_DATOS" -eq 1 ]; then
    echo "Usuarios:     ADM-001/admin123  MES-001/mesero123  COC-001/cocina123  CAJ-001/cajero123"
else
    echo "Datos:        vuelva a correrlo con --recrear --con-datos-prueba para sembrar datos de ejemplo"
fi
