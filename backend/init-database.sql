-- ================================================
-- Script de inicialización de PostgreSQL
-- Base de datos para AFA Fullstack Application
-- ================================================

-- Crear la base de datos
CREATE DATABASE afa_fullstack_db
    WITH
    OWNER = afa_user
    ENCODING = 'UTF8'
    LC_COLLATE = 'es_ES.UTF-8'
    LC_CTYPE = 'es_ES.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

COMMENT ON DATABASE afa_fullstack_db IS 'Base de datos para la aplicación AFA Fullstack';

-- Conectar a la base de datos
\c afa_fullstack_db

-- Crear extensiones útiles (opcional)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Nota: Las tablas serán creadas automáticamente por Hibernate
-- gracias a la configuración spring.jpa.hibernate.ddl-auto=update
