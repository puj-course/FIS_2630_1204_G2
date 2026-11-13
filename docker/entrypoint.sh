#!/usr/bin/env bash
# Levanta la pantalla virtual, el servidor VNC, el cliente web y la aplicación.
set -euo pipefail

: "${PGPASSWORD:?Defina PGPASSWORD (contraseña de PostgreSQL)}"

Xvfb "$DISPLAY" -screen 0 "${RESOLUCION:-1280x800x24}" -nolisten tcp &
sleep 1

x11vnc -display "$DISPLAY" -forever -shared -nopw -quiet -rfbport 5900 -localhost &
websockify --web=/usr/share/novnc 6080 localhost:5900 &

echo ">> GastroFlow disponible en http://localhost:6080/vnc.html"

# prism.order=sw: render por software, porque el contenedor no tiene GPU.
exec java -Dprism.order=sw -Djava.awt.headless=false \
     --module-path /app/javafx --add-modules javafx.controls,javafx.fxml \
     -cp "/app/app.jar:/app/lib/*" Main