# Spring Boot Multitenant Demo API

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

## Error Handling

If you provide an invalid tenant name (containing characters other than alphanumeric and underscores), you'll receive an
error response.

Example of an invalid request:

```bash
# This will fail because it contains a special character
curl -X POST "http://localhost:8080/tenants/create?tenantName=company-1"
```

## Docker Environment

This application is configured to work with Docker Compose. The database connection details are:

- Database: PostgreSQL
- Port: 5433
- Database Name: tenant_db
- Username: yu71
- Password: 53cret