-- Runs on first container start, against POSTGRES_DB (tenant_db) as POSTGRES_USER (yu71).
-- The database itself is created by the postgres entrypoint, so only schemas are needed here.
CREATE SCHEMA IF NOT EXISTS tenant_a;
CREATE SCHEMA IF NOT EXISTS tenant_b;

GRANT USAGE ON SCHEMA tenant_a TO yu71;
GRANT USAGE ON SCHEMA tenant_b TO yu71;
