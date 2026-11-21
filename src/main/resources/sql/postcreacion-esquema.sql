-- Se ejecuta en cada arranque, después de esquema.sql.
-- Contiene solo lo que no se puede expresar con anotaciones JPA. Todo es idempotente (IF NOT EXISTS).

-- Extensiones para buscar hermanos por nombre sin tildes y con coincidencia parcial
CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Búsqueda parcial por nombre y apellidos (trigram)
CREATE INDEX IF NOT EXISTS ix_hermano_nombre_trgm ON hermano USING gin (nombre_normalizado gin_trgm_ops);

-- El código de túnica es único solo entre las que siguen en el inventario (se reutiliza al pasar a PROPIEDAD_HERMANO)
CREATE UNIQUE INDEX IF NOT EXISTS ux_tunica_codigo_inventario ON tunica (codigo) WHERE estado <> 'PROPIEDAD_HERMANO';

-- Un único reparto abierto
CREATE UNIQUE INDEX IF NOT EXISTS ux_reparto_abierto ON reparto (estado) WHERE estado = 'ABIERTO';

-- Que los 2 priostes no entreguen la misma túnica a la vez
CREATE UNIQUE INDEX IF NOT EXISTS ux_alquiler_tunica_activa ON alquiler (tunica_id) WHERE estado = 'ACTIVO';

-- Una sola túnica activa por hermano y reparto
CREATE UNIQUE INDEX IF NOT EXISTS ux_alquiler_hermano_activo ON alquiler (reparto_id, hermano_id) WHERE estado = 'ACTIVO';

-- Un único resguardo vigente por alquiler
CREATE UNIQUE INDEX IF NOT EXISTS ux_resguardo_vigente ON resguardo (alquiler_id) WHERE NOT anulado;

-- Cada concepto de sanción se cobra una sola vez por alquiler (el importe A incluido)
CREATE UNIQUE INDEX IF NOT EXISTS ux_sancion_concepto ON sancion (alquiler_id, concepto) WHERE estado <> 'ANULADA';

-- Consulta rápida de sanciones pendientes de un hermano
CREATE INDEX IF NOT EXISTS ix_sancion_hermano_pendiente ON sancion (hermano_id) WHERE estado = 'PENDIENTE';
