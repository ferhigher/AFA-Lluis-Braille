#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"
PID_FILE="$SCRIPT_DIR/.backend.pid"
LOG_FILE="$SCRIPT_DIR/logs/backend.log"

if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    echo "El backend ya está corriendo (PID $(cat "$PID_FILE"))"
    exit 0
fi

if [ ! -f "$ENV_FILE" ]; then
    echo "ERROR: No se encontró el archivo .env"
    echo "Crea uno a partir de .env.example:"
    echo "  cp .env.example .env"
    exit 1
fi

mkdir -p "$SCRIPT_DIR/logs"

set -a
source "$ENV_FILE"
set +a

echo "Arrancando backend..."
cd "$SCRIPT_DIR/backend"
mvn spring-boot:run >> "$LOG_FILE" 2>&1 &
echo $! > "$PID_FILE"
echo "Backend arrancando con PID $(cat "$PID_FILE")"
echo "Disponible en http://localhost:8080"
echo "Logs: tail -f logs/backend.log"
