#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$SCRIPT_DIR/frontend"
PID_FILE="$SCRIPT_DIR/.frontend.pid"
LOG_FILE="$SCRIPT_DIR/logs/frontend.log"

if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    echo "El frontend ya está corriendo (PID $(cat "$PID_FILE"))"
    exit 0
fi

mkdir -p "$SCRIPT_DIR/logs"

if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
    echo "Instalando dependencias..."
    cd "$FRONTEND_DIR" && npm install
fi

echo "Arrancando frontend..."
cd "$FRONTEND_DIR"
npm run dev >> "$LOG_FILE" 2>&1 &
echo $! > "$PID_FILE"
echo "Frontend arrancando con PID $(cat "$PID_FILE")"
echo "Disponible en http://localhost:5173"
echo "Logs: tail -f logs/frontend.log"
