# Spring Boot Multitenant Demo API

[![Java CI with Gradle](https://github.com/hendisantika/spring-boot-multi-tenant-postgresql/actions/workflows/gradle.yml/badge.svg)](https://github.com/hendisantika/spring-boot-multi-tenant-postgresql/actions/workflows/gradle.yml)

This document provides examples of how to interact with the Multitenant Demo API using cURL commands.

## Prerequisites

- The application should be running locally on port 8080
- cURL should be installed on your system

## Tenant Management

### Create a New Tenant

Creates a new tenant by generating a new schema in the database.

```bash
# Create a tenant named 'company1'
curl -X POST "http://localhost:8080/tenants/create?tenantName=company1"

# Create a tenant named 'company2'
curl -X POST "http://localhost:8080/tenants/create?tenantName=company2"
```

#### Parameters:

- `tenantName`: The name of the tenant to create. Must contain only alphanumeric characters and underscores.

#### Response:

A success message confirming the tenant was created.

Example:

```
Tenant company1 created successfully
```

## Customer Management

CRUD over the `customers` table of one tenant. The tenant comes from the
`X-TenantID` header; without it the request lands on the `public` schema.

```bash
# create
curl -X POST "http://localhost:8080/customers" \
  -H "Content-Type: application/json" \
  -H "X-TenantID: tenant_a" \
  -d '{"name":"Alice","email":"alice@example.com"}'

# list (paged) / search / read one
curl "http://localhost:8080/customers"                              -H "X-TenantID: tenant_a"
curl "http://localhost:8080/customers?page=1&size=50&sort=name,asc" -H "X-TenantID: tenant_a"
curl "http://localhost:8080/customers?q=alice"                      -H "X-TenantID: tenant_a"
curl "http://localhost:8080/customers/1"                            -H "X-TenantID: tenant_a"

# update
curl -X PUT "http://localhost:8080/customers/1" \
  -H "Content-Type: application/json" \
  -H "X-TenantID: tenant_a" \
  -d '{"name":"Alice Updated","email":"alice.new@example.com"}'

# delete
curl -X DELETE "http://localhost:8080/customers/1" -H "X-TenantID: tenant_a"
```

| Method | Path              | Success | Errors                                    |
|--------|-------------------|---------|-------------------------------------------|
| POST   | `/customers`      | 201     | 400 invalid body, 409 duplicate email     |
| GET    | `/customers`      | 200     | paged, see below                          |
| GET    | `/customers/{id}` | 200     | 404 unknown id                            |
| PUT    | `/customers/{id}` | 200     | 400 invalid body, 404 unknown id          |
| DELETE | `/customers/{id}` | 204     | 404 unknown id                            |

### Paging the list

`GET /customers` is paged, so a tenant with a large table is never dumped in one
response. It takes the usual Spring Data parameters:

| Parameter | Default | Notes                                            |
|-----------|---------|--------------------------------------------------|
| `q`       | none    | substring of name or email, case-insensitive      |
| `page`    | `0`     | zero-indexed                                      |
| `size`    | `20`    | clamped to 100                                    |
| `sort`    | `id`    | `field,asc` / `field,desc`, repeatable            |

`q` filters before paging, so `totalElements` counts the matches rather than the
whole table, and it combines freely with `page`, `size` and `sort`. A blank `q`
is treated as no filter. `%` and `_` are matched literally, so searching for
`50%` finds that text instead of every row.

Search is backed by `pg_trgm` GIN indexes on `LOWER(name)` and `LOWER(email)`,
added per tenant schema by `V2`. A leading-wildcard `LIKE` cannot use a B-tree,
so without them every search is a sequential scan. Two caveats: trigram indexes
need a term of **at least 3 characters** (shorter terms fall back to a scan), and
on a small table the planner will still choose a scan because it is genuinely
cheaper - the index starts winning in the hundreds of thousands of rows.

Sortable fields are `id`, `name`, `email` and `createdAt`. Anything else gives
`400` naming the offending field, rather than a `500` from the query builder.
Note that a malformed direction (`?sort=name,sideways`) is read by Spring Data
as a second field, so it is rejected the same way.

```json
{
  "content": [
    { "id": 1, "name": "Alice", "email": "alice@example.com", "createdAt": "..." }
  ],
  "page": { "size": 20, "number": 0, "totalElements": 1, "totalPages": 1 }
}
```

Rows never cross tenants: the unique index on `email` lives in each schema, so
the same address can exist once per tenant.

## Notes

- The tenant name must contain only alphanumeric characters and underscores.
- Each tenant gets its own schema in the PostgreSQL database.
- The application uses schema-based multitenancy, where each tenant's data is isolated in its own schema within the same
  database.

## Database Migrations

Schema changes are managed by Flyway. Because every tenant owns a schema, the
migrations in `src/main/resources/db/migration` are applied **once per tenant
schema** rather than once per database, so each schema carries its own
`flyway_schema_history` table.

Migrations run in two places:

- at startup, against every existing schema (including `public`, which backs the
  default tenant)
- immediately after `POST /tenants/create`, so a brand new tenant starts at V1

Spring Boot's built-in Flyway auto-configuration only ever targets one schema, so
it is disabled (`spring.flyway.enabled=false`) and `TenantSchemaMigrator` drives
the migrations instead.

### Naming convention

```
Vx_DDMMYYYY_HHMM__DESCRIPTION.sql
```

For example `V1_09082026_0836__create_customers_table.sql`, which Flyway reads as
version `1.09082026.0836`. Write table names unqualified - Flyway sets the target
schema before the script runs.

## Error Handling

If you provide an invalid tenant name (containing characters other than alphanumeric and underscores), the request is
rejected with `400 Bad Request`.

Example of an invalid request:

```bash
# This will fail because it contains a special character
curl -X POST "http://localhost:8080/tenants/create?tenantName=company-1"
```

## Docker Environment

This application is configured to work with Docker Compose. The database connection details are:

- Database: PostgreSQL
- Port: 5434 (host) -> 5432 (container)
- Database Name: tenant_db
- Username: yu71
- Password: 53cret

pgAdmin is published on http://localhost:5052 (admin@pgadmin.com / admin).

If those host ports clash with something else on your machine, override them without
editing any files:

```bash
# start the stack on different host ports
POSTGRES_PORT=15432 PGADMIN_PORT=15050 docker compose up -d

# and point the app at the same port
DB_PORT=15432 ./gradlew bootRun
```

`DB_HOST`, `DB_PORT` and `DB_NAME` all fall back to the defaults above when unset.

## Running the App in Docker

The app has its own image and a healthcheck. It sits behind a compose profile
because running the app locally starts this compose file itself, and an
always-on app service would boot a second copy and collide on port 8080.

```bash
# whole stack, app included
docker compose --profile app up -d --build

# just the dependencies, which is what ./gradlew bootRun expects
docker compose up -d
```

`APP_PORT` overrides the published port (default 8080).

### Healthcheck

The image declares a `HEALTHCHECK` that polls `/actuator/health`, so it applies
whether the container is started by compose or by `docker run`:

```bash
docker inspect -f '{{.State.Health.Status}}' multitenant-app
```

It probes Actuator rather than the TCP port, so it reports the app as unhealthy
when the database is unreachable, not merely when the JVM has died. Losing
Postgres flips the container to `unhealthy` and getting it back flips it to
`healthy` again. `start-period` allows 60s for JVM startup and the per-tenant
Flyway migrations.

Only the `health` endpoint is exposed and it reports status without details, so
the probe is safe to leave unauthenticated; anything else under `/actuator`
returns 404.

## Reference

Based on this [article](https://towardsdev.com/multi-tenant-architecture-using-springboot-and-postgresql-d3d800e44ab0)