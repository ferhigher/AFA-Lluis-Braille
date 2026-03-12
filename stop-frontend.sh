#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$SCRIPT_DIR/.frontend.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "No se encontró archivo PID. ¿Está el frontend corriendo?"
    exit 0
fi

PID=$(cat "$PID_FILE")

if ! kill -0 "$PID" 2>/dev/null; then
    echo "El proceso $PID no existe. Limpiando PID file..."
    rm -f "$PID_FILE"
    exit 0
fi

echo "Deteniendo frontend (PID $PID)..."
kill "$PID"

for i in $(seq 1 5); do
    if ! kill -0 "$PID" 2>/dev/null; then
        break
    fi
    sleep 1
done

if kill -0 "$PID" 2>/dev/null; then
    kill -9 "$PID"
fi

rm -f "$PID_FILE"
echo "Frontend detenido."
