# Foundation Module

The foundation module provides the core infrastructure for the Mini-PaaS platform, including API server, database management, and Docker integration.

## Features

- **REST API Server** - Built with Ktor for handling application management requests
- **Database Layer** - PostgreSQL with Exposed ORM for persistent storage
- **Docker Integration** - Docker Java client for container operations
- **Data Models** - App, Deployment, and Container entities with full lifecycle management

## Architecture

### Components

- **Application.kt** - Main entry point and Ktor server configuration
- **DatabaseFactory** - Database connection pooling and schema initialization
- **Repositories** - Data access layer for database operations
- **Services** - Business logic including Docker service
- **Routes** - REST API endpoints
- **Models** - Domain models and DTOs

### Database Schema

**apps**
- id (primary key)
- name
- git_url
- branch
- env (JSON)
- status
- created_at, updated_at

**deployments**
- id (primary key)
- app_id (foreign key)
- image_tag
- replicas
- status
- created_at, updated_at

**containers**
- id (primary key)
- deployment_id (foreign key)
- app_id (foreign key)
- container_id (Docker ID)
- port
- status
- created_at, updated_at

## API Endpoints

### Apps

- `POST /api/apps` - Create a new application
- `GET /api/apps` - List all applications
- `GET /api/apps/{id}` - Get application by ID
- `PUT /api/apps/{id}` - Update application
- `DELETE /api/apps/{id}` - Delete application

### Health

- `GET /health` - Health check endpoint (checks Docker connectivity)

## Running the Module

### Prerequisites

1. Java 21 or higher
2. Docker running locally
3. PostgreSQL database (can use docker-compose)

### Start Dependencies

```bash
# From project root
docker-compose up -d
```

This starts PostgreSQL and Redis containers.

### Run the Application

```bash
# From project root
./gradlew :foundation:run
```

Or with custom configuration:

```bash
export PORT=8080
export DATABASE_URL=jdbc:postgresql://localhost:5432/paas
export DATABASE_USER=paas
export DATABASE_PASSWORD=paas
./gradlew :foundation:run
```

### Build

```bash
./gradlew :foundation:build
```

## Testing the API

### Create an App

```bash
curl -X POST http://localhost:8080/api/apps \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my-app",
    "gitUrl": "https://github.com/user/repo.git",
    "branch": "main",
    "env": {
      "PORT": "3000",
      "NODE_ENV": "production"
    }
  }'
```

### List Apps

```bash
curl http://localhost:8080/api/apps
```

### Get App by ID

```bash
curl http://localhost:8080/api/apps/{app-id}
```

### Update App

```bash
curl -X PUT http://localhost:8080/api/apps/{app-id} \
  -H "Content-Type: application/json" \
  -d '{
    "branch": "develop",
    "env": {
      "PORT": "4000"
    }
  }'
```

### Delete App

```bash
curl -X DELETE http://localhost:8080/api/apps/{app-id}
```

### Health Check

```bash
curl http://localhost:8080/health
```

## Configuration

Configuration is managed through `application.conf` and environment variables. See `.env.example` in the project root for available environment variables.

## Next Steps

- Implement deployment creation and management
- Add container lifecycle operations
- Implement build pipeline integration
- Add authentication and authorization
