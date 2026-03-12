#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$SCRIPT_DIR/.backend.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "No se encontró archivo PID. ¿Está el backend corriendo?"
    exit 0
fi

PID=$(cat "$PID_FILE")

if ! kill -0 "$PID" 2>/dev/null; then
    echo "El proceso $PID no existe. Limpiando PID file..."
    rm -f "$PID_FILE"
    exit 0
fi

echo "Deteniendo backend (PID $PID)..."
kill "$PID"

# Esperar hasta 10s a que termine limpiamente
for i in $(seq 1 10); do
    if ! kill -0 "$PID" 2>/dev/null; then
        break
    fi
    sleep 1
done

# Si sigue vivo, forzar
if kill -0 "$PID" 2>/dev/null; then
    echo "Forzando parada..."
    kill -9 "$PID"
fi

rm -f "$PID_FILE"
echo "Backend detenido."
