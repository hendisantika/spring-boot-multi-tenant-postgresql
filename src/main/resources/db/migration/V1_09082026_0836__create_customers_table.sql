-- Applied once per tenant schema. Flyway sets the target schema before running
-- this, so the table name is deliberately unqualified.
CREATE TABLE customers
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_customers_email ON customers (email) WHERE email IS NOT NULL;
