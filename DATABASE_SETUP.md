# Database Setup Guide

## 🐘 PostgreSQL Configuration

### Database Details
- **Database Name:** flowiq
- **Username:** flowiq
- **Password:** flowiq123
- **Port:** 5432
- **Host:** localhost

---

## 🚀 Quick Start

### Option 1: Auto-start with Spring Boot (Recommended)

1. **Start Docker Desktop** (if not running)
   - Open Docker Desktop application
   - Wait until it shows "Docker Desktop is running"

2. **Run Spring Boot application:**
   ```bash
   cd flowiq-backend
   ./mvnw spring-boot:run
   ```

3. **Spring Boot will automatically:**
   - Start PostgreSQL container via Docker Compose
   - Create database `flowiq`
   - Run migrations
   - Connect to database

✅ **That's it!** Database is ready.

---

### Option 2: Manual Docker Compose

If you prefer to start database separately:

```bash
# Start PostgreSQL
docker-compose up -d

# Check if running
docker ps

# View logs
docker-compose logs -f

# Stop
docker-compose down

# Stop and remove data
docker-compose down -v
```

---

## 🔍 Verify Database Connection

### Check Docker Container
```bash
docker ps | findstr flowiq-postgres
```

Should show:
```
flowiq-postgres   postgres:15-alpine   Up X minutes   0.0.0.0:5432->5432/tcp
```

### Connect to Database
```bash
# Using psql
docker exec -it flowiq-postgres psql -U flowiq -d flowiq

# Or using any PostgreSQL client
Host: localhost
Port: 5432
Database: flowiq
Username: flowiq
Password: flowiq123
```

### Check Tables
```sql
-- List all tables
\dt

-- Check users table structure
\d users
```

---

## 📊 Database Schema

### Users Table
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    company VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN DEFAULT true,
    email_verified BOOLEAN DEFAULT false,
    avatar_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);
```

---

## ⚙️ Configuration

### application.properties
```properties
# Database Connection
spring.datasource.url=jdbc:postgresql://localhost:5432/flowiq
spring.datasource.username=flowiq
spring.datasource.password=flowiq123

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Docker Compose
spring.docker.compose.enabled=true
```

### compose.yaml
```yaml
services:
  postgres:
    image: postgres:15-alpine
    container_name: flowiq-postgres
    environment:
      POSTGRES_DB: flowiq
      POSTGRES_USER: flowiq
      POSTGRES_PASSWORD: flowiq123
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
```

---

## 🛠️ Troubleshooting

### Error: "Docker daemon is not running"
**Solution:** Start Docker Desktop application

### Error: "Port 5432 is already in use"
**Solution:** 
```bash
# Find process using port 5432
netstat -ano | findstr :5432

# Kill process (replace PID)
taskkill /PID <PID> /F

# Or stop other PostgreSQL services
```

### Error: "Connection refused"
**Solution:**
1. Check Docker container is running: `docker ps`
2. Check logs: `docker-compose logs postgres`
3. Restart container: `docker-compose restart`

### Database not created
**Solution:**
```bash
# Connect to container
docker exec -it flowiq-postgres bash

# Create database manually
psql -U flowiq
CREATE DATABASE flowiq;
\q
```

---

## 🔄 Reset Database

To start fresh:

```bash
# Stop and remove everything
docker-compose down -v

# Start again
docker-compose up -d

# Or let Spring Boot recreate on next run
./mvnw spring-boot:run
```

---

## 📝 Development Notes

### Hibernate DDL Mode

Current setting: `spring.jpa.hibernate.ddl-auto=update`

**Options:**
- `update` - Update schema (safe for development)
- `create` - Drop and recreate schema on every start
- `create-drop` - Create on start, drop on stop
- `validate` - Only validate schema
- `none` - Do nothing

**For Production:** Use Flyway or Liquibase migrations instead!

### Show SQL

`spring.jpa.show-sql=true` - Logs all SQL queries to console

Useful for debugging, but disable in production.

---

## ✅ Success Indicators

When database is properly configured, you should see in Spring Boot logs:

```
Tomcat started on port 8080
HikariPool-1 - Start completed.
Hibernate: create table users (...)
Started FlowiqBackendApplication in X.XXX seconds
```

No errors about DataSource or database connection!

---

## 🔐 Security Notes

**⚠️ WARNING:** Current credentials are for DEVELOPMENT ONLY!

Before production:
1. Change all passwords
2. Use environment variables
3. Enable SSL for database connection
4. Restrict database access
5. Use proper secrets management

Example for production:
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```

---

**Status:** ✅ Ready for Development  
**Last Updated:** June 9, 2026
