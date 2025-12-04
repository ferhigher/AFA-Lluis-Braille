#!/bin/bash

# ================================================
# Script de configuración de PostgreSQL
# Para AFA Fullstack Application
# ================================================

echo "================================================"
echo "Configuración de PostgreSQL para AFA Fullstack"
echo "================================================"
echo ""

# Verificar si PostgreSQL está instalado
if ! command -v psql &> /dev/null; then
    echo "❌ PostgreSQL no está instalado."
    echo ""
    echo "Para instalar PostgreSQL:"
    echo "  Ubuntu/Debian: sudo apt-get install postgresql postgresql-contrib"
    echo "  Fedora/RHEL:   sudo dnf install postgresql postgresql-server"
    echo "  macOS:         brew install postgresql"
    echo ""
    exit 1
fi

echo "✓ PostgreSQL está instalado"
echo ""

# Verificar si el servicio está corriendo
if ! sudo systemctl is-active --quiet postgresql; then
    echo "⚠️  El servicio PostgreSQL no está corriendo."
    echo "Intentando iniciar el servicio..."
    sudo systemctl start postgresql
    if [ $? -eq 0 ]; then
        echo "✓ Servicio PostgreSQL iniciado"
    else
        echo "❌ No se pudo iniciar el servicio PostgreSQL"
        exit 1
    fi
else
    echo "✓ Servicio PostgreSQL está corriendo"
fi

echo ""
echo "================================================"
echo "Creando usuario y base de datos..."
echo "================================================"
echo ""

# Crear usuario y base de datos
sudo -u postgres psql << EOF
-- Crear el usuario si no existe
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_user WHERE usename = 'afa_user') THEN
        CREATE USER afa_user WITH PASSWORD 'afa_password';
        RAISE NOTICE 'Usuario afa_user creado';
    ELSE
        RAISE NOTICE 'Usuario afa_user ya existe';
    END IF;
END
\$\$;

-- Crear la base de datos si no existe
SELECT 'CREATE DATABASE afa_fullstack_db OWNER afa_user'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'afa_fullstack_db')\gexec

-- Otorgar privilegios
GRANT ALL PRIVILEGES ON DATABASE afa_fullstack_db TO afa_user;

-- Conectar a la base de datos y otorgar privilegios en el schema
\c afa_fullstack_db
GRANT ALL ON SCHEMA public TO afa_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO afa_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO afa_user;

EOF

if [ $? -eq 0 ]; then
    echo ""
    echo "================================================"
    echo "✅ Configuración completada exitosamente"
    echo "================================================"
    echo ""
    echo "Detalles de la conexión:"
    echo "  Base de datos: afa_fullstack_db"
    echo "  Usuario:       afa_user"
    echo "  Password:      afa_password"
    echo "  Host:          localhost"
    echo "  Puerto:        5432"
    echo ""
    echo "Ahora puedes iniciar tu aplicación Spring Boot."
    echo ""
else
    echo ""
    echo "❌ Hubo un error durante la configuración"
    exit 1
fi
