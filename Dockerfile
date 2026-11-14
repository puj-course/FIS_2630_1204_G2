# syntax=docker/dockerfile:1

# =============================================================================
# GastroFlow - imagen de la aplicación de escritorio (JavaFX) con acceso web
#
# GastroFlow no es un servicio web: es una aplicación con ventanas. Un
# contenedor no tiene pantalla, así que dentro se levanta una pantalla virtual
# (Xvfb) y se expone por el navegador con noVNC en el puerto 6080.
#
# Dos etapas:
#   1. build   -> compila con Maven y reúne el jar y sus bibliotecas.
#   2. runtime -> solo JRE + pantalla virtual + el resultado de la etapa 1.
# =============================================================================

# ---------- Etapa 1: construcción del artefacto -------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Primero solo el pom: mientras no cambien las dependencias, Docker reutiliza
# esta capa y no vuelve a descargar medio internet en cada build.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q dependency:go-offline

# Después el código. Las pruebas ya corren en el pipeline de CI (necesitan una
# base de datos), así que aquí solo se empaqueta.
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -q -DskipTests package \
 && mvn -B -q dependency:copy-dependencies \
        -DoutputDirectory=target/lib -DincludeScope=runtime \
 && cp target/VisualMesas-*.jar target/app.jar

# ---------- Etapa 2: imagen final --------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

# Pantalla virtual + VNC + cliente web, y las bibliotecas nativas que JavaFX
# necesita en Linux (GTK, OpenGL por software, fuentes).
RUN apt-get update \
 && apt-get install -y --no-install-recommends \
	xvfb x11vnc novnc websockify curl procps openbox \
        xvfb x11vnc novnc websockify curl procps \
        libgtk-3-0 libgl1 libgl1-mesa-dri libxtst6 libxxf86vm1 libasound2 \
        fonts-dejavu-core \
 && rm -rf /var/lib/apt/lists/*

# No se ejecuta como root.
RUN useradd --create-home --uid 1001 gastroflow
WORKDIR /app

COPY --from=build /build/target/app.jar ./app.jar
COPY --from=build /build/target/lib ./lib
COPY docker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN mkdir /app/javafx && mv /app/lib/javafx-*.jar /app/javafx/
RUN sed -i 's/\r$//' /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh && chown -R gastroflow:gastroflow /app

USER gastroflow

ENV DISPLAY=:99 \
    RESOLUCION=1280x800x24 \
    PGHOST=db \
    PGPORT=5432 \
    PGDATABASE=gastroflow \
    PGUSER=postgres
# PGPASSWORD no se fija aquí: llega por variable de entorno al ejecutar.

# noVNC (la aplicación vista desde el navegador)
EXPOSE 6080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD pgrep -f "Main" >/dev/null && curl -fsS http://localhost:6080/vnc.html >/dev/null || exit 1

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
