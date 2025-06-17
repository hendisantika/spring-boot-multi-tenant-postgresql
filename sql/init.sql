CREATE
DATABASE tenant_db if NOT exist;
-- Create multiple schemas as per your requirement
CREATE SCHEMA tenant_a;
CREATE SCHEMA tenant_b;

GRANT
USAGE
ON
SCHEMA
tenant_a TO yu71;
GRANT USAGE ON SCHEMA
tenant_b TO yu71;