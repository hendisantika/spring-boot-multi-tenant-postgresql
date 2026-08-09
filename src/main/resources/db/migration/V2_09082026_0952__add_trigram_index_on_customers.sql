-- Speeds up the ?q= search, which is LOWER(col) LIKE '%term%'. A leading
-- wildcard rules out a B-tree, so it needs trigrams.

-- The extension is database-wide. Pin it to public rather than letting it land
-- in whichever tenant schema happens to migrate first, and qualify the operator
-- class below because the runtime search_path holds only the tenant's schema.
CREATE EXTENSION IF NOT EXISTS pg_trgm SCHEMA public;

-- Indexed on LOWER(...) to match how the query compares, so the planner can use
-- these rather than re-evaluating the function per row.
CREATE INDEX idx_customers_name_trgm
    ON customers USING gin (LOWER(name) public.gin_trgm_ops);

CREATE INDEX idx_customers_email_trgm
    ON customers USING gin (LOWER(email) public.gin_trgm_ops);
