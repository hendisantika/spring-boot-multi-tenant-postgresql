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

## Reference

Based on this [article](https://towardsdev.com/multi-tenant-architecture-using-springboot-and-postgresql-d3d800e44ab0)